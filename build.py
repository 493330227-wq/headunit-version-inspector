#!/usr/bin/env python3
"""Offline build. Requires JDK 17, Android SDK platform 35 and build-tools 35.
JAVA_HOME, ANDROID_JAR, ANDROID_BUILD_TOOLS, SIGNING_KEYSTORE must be set.
Keystore password is read from SIGNING_PASSWORD; never committed.
"""
import os, pathlib, subprocess, shutil, zipfile, hashlib, json, xml.etree.ElementTree as ET
root=pathlib.Path(__file__).resolve().parent
java=pathlib.Path(os.environ['JAVA_HOME'])/'bin'
bt=pathlib.Path(os.environ['ANDROID_BUILD_TOOLS'])
android=pathlib.Path(os.environ['ANDROID_JAR'])
key=pathlib.Path(os.environ['SIGNING_KEYSTORE'])
manifest=ET.parse(root/'AndroidManifest.xml').getroot()
version=manifest.attrib['{http://schemas.android.com/apk/res/android}versionName']
build=root/'build'
if build.exists():shutil.rmtree(build)
(build/'classes').mkdir(parents=True);(build/'dex').mkdir()
def run(*args):subprocess.run([str(a) for a in args],check=True)
run(bt/'aapt2','compile','--dir',root/'res','-o',build/'resources.zip')
run(bt/'aapt2','link','-I',android,'--manifest',root/'AndroidManifest.xml','-o',build/'base.apk',build/'resources.zip')
run(java/'javac','-encoding','UTF-8','-source','8','-target','8','-bootclasspath',android,'-d',build/'classes',*sorted((root/'src').rglob('*.java')))
run(java/'jar','cf',build/'classes.jar','-C',build/'classes','.')
run(bt/'d8','--min-api','14','--lib',android,'--output',build/'dex',build/'classes.jar')
with zipfile.ZipFile(build/'base.apk','a',zipfile.ZIP_DEFLATED) as z:
 for f in (build/'dex').glob('*.dex'):z.write(f,f.name)
run(bt/'zipalign','-f','4',build/'base.apk',build/'aligned.apk')
apk=root/('车机版本检测-'+version+'.apk')
run(bt/'apksigner','sign','--ks',key,'--ks-key-alias','headunit','--ks-pass','env:SIGNING_PASSWORD','--v1-signing-enabled','true','--v2-signing-enabled','true','--v3-signing-enabled','true','--out',apk,build/'aligned.apk')
run(bt/'apksigner','verify','--verbose','--print-certs','--min-sdk-version','14',apk)
run(bt/'zipalign','-c','4',apk)
run(java/'javac','-encoding','UTF-8','-source','8','-target','8','-d',build/'classes',root/'src/cn/headunit/inspector/Rules.java',root/'tests/RulesTest.java')
run(java/'java','-cp',build/'classes','cn.headunit.inspector.RulesTest')
run(java/'javac','-encoding','UTF-8','-cp',build/'classes','-d',build/'classes',root/'tests/DemoFixtures.java')
fixtures=subprocess.check_output([str(java/'java'),'-cp',str(build/'classes'),'cn.headunit.inspector.DemoFixtures'],text=True)
assert len(json.loads(fixtures)) == 11
(root/'demo-fixtures.json').write_text(fixtures)
(root/'演示页面.html').write_text((root/'demo-template.html').read_text().replace('__FIXTURES__',fixtures))
with zipfile.ZipFile(apk) as z:assert z.testzip() is None
meta={'file':apk.name,'bytes':apk.stat().st_size,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'minSdk':14,'targetSdk':35,'version':version,'package':'cn.headunit.inspector','permissions':[]}
(root/'构建校验.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2))
print(json.dumps(meta,ensure_ascii=False,indent=2))
