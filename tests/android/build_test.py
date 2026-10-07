"""Build an instrumentation APK using the same signing environment as build.py.
Install the production APK and this test APK, then run:
adb shell am instrument -w cn.headunit.inspector.tests/.SmokeTest
Requires an Android API 28+ test device. Never installs on a device automatically.
"""
import os,pathlib,subprocess,zipfile
root=pathlib.Path(__file__).resolve().parents[2]
out=root/'build/native-tests';(out/'classes').mkdir(parents=True,exist_ok=True);(out/'dex').mkdir(exist_ok=True)
java=pathlib.Path(os.environ['JAVA_HOME'])/'bin';bt=pathlib.Path(os.environ['ANDROID_BUILD_TOOLS']);android=os.environ['ANDROID_JAR']
def run(*a):subprocess.run(list(map(str,a)),check=True)
run(bt/'aapt2','link','-I',android,'--manifest',root/'tests/android/AndroidManifest.xml','-o',out/'base.apk')
run(java/'javac','-encoding','UTF-8','-source','8','-target','8','-bootclasspath',android,'-d',out/'classes',root/'tests/android/SmokeTest.java')
run(java/'jar','cf',out/'classes.jar','-C',out/'classes','.')
run(bt/'d8','--min-api','28','--lib',android,'--output',out/'dex',out/'classes.jar')
with zipfile.ZipFile(out/'base.apk','a',zipfile.ZIP_DEFLATED) as z:z.write(out/'dex/classes.dex','classes.dex')
run(bt/'zipalign','-f','4',out/'base.apk',out/'aligned.apk')
run(bt/'apksigner','sign','--ks',os.environ['SIGNING_KEYSTORE'],'--ks-key-alias','headunit','--ks-pass','env:SIGNING_PASSWORD','--out',out/'tests.apk',out/'aligned.apk')
