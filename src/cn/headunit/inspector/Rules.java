package cn.headunit.inspector;
public final class Rules {
 public static final String RULE_VERSION="2026-10-07.3";
 public static String version(int api) {
  switch(api){case 14:case 15:return "4.0";case 16:return "4.1";case 17:return "4.2";case 18:return "4.3";case 19:case 20:return "4.4";case 21:return "5.0";case 22:return "5.1";case 23:return "6.0";case 24:return "7.0";case 25:return "7.1";case 26:return "8.0";case 27:return "8.1";case 28:return "9";case 29:return "10";case 30:return "11";case 31:case 32:return "12";case 33:return "13";case 34:return "14";case 35:return "15";case 36:return "16";case 37:return "17";default:return "未知版本（API "+api+"）";}
 }
 public static String inferredVersion(int api, boolean conflict) {
  if(conflict || version(api).startsWith("未知")) return "无法确定";
  return "Android "+version(api);
 }
 public static boolean matches(String release,int api){
  String expected=version(api);if(expected.startsWith("未知"))return true;
  if(release==null)return false;
  // Android 9.1 is not an official release: only accept zero suffixes
  // for integer-version Android releases, not arbitrary minor versions.
  if(api>=28){
   if(api==32 && (release.equals("12L")||release.matches("12\\.1(\\.0)?")))return true;
   return release.matches(expected+"(\\.0){0,2}");
  }
  return release.equals(expected)||release.startsWith(expected+".");
 }
 public static String[] recommend(int api, boolean conflict,boolean abiSupported){
  if(version(api).startsWith("未知"))return new String[]{"暂无法可靠推荐","系统 API 尚未收录，无法推定版本。","",""};
  if(conflict)return new String[]{"暂无法可靠推荐","系统 API 与接口证据冲突，请查看检测依据。","",""};
  if(!abiSupported)return new String[]{"暂无法可靠推荐","未确认 CPU 架构与目录中的安装包匹配。","",""};
  if(api>=28)return new String[]{"官方 DiPlay · 0.2.12 预览版","适合 Android 9 及以上的系统门槛。此版本为公开预览版，实际连接需验证。","DiPlay-0.2.12.apk","https://github.com/shihabal3amri/DiPlay/releases/tag/v0.2.12"};
  if(api>=26)return new String[]{"KrunkZhou · Android 8 兼容版","适合 Android 8.0 / 8.1 的系统门槛。社区兼容版，安装后请验证本机的 CarPlay 连接。","DiPlay-Android8-complete-debug.apk","https://github.com/KrunkZhou/DiPlay-Android8.0/releases/tag/v0.2.6"};
  if(api>=19)return new String[]{"Legacy Android · 4.4 兼容版","适合 Android 4.4 至 7.1 的系统门槛。社区试用包，实际连接仍需验证。","DiPlay-Legacy-Android-v0.2.7.apk","https://github.com/programmerguohuajing/DiPlay-Legacy-Android/releases/tag/v0.2.7"};
  return new String[]{"没有已核验的适配包","当前目录最低要求 API 19（Android 4.4）。","",""};
 }
}
