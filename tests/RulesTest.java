package cn.headunit.inspector;
public class RulesTest {
 static int checks=0;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  check(Rules.recommend(18,false,true)[2].isEmpty(),"below minimum");
  for(int api=19;api<=24;api++)check(Rules.recommend(api,false,true)[2].equals("DiPlay-Legacy-Android-v0.2.7.apk"),"legacy "+api);
  for(int api=25;api<=37;api++)check(Rules.recommend(api,false,true)[2].equals("DiPlay-0.2.15.apk"),"official "+api);
  for(int api=14;api<=37;api++){check(Rules.recommend(api,true,true)[2].isEmpty(),"hold conflict "+api);check(Rules.recommend(api,false,false)[2].isEmpty(),"hold ABI "+api);}
  check(!Rules.matches("9.1",27),"spoofed label");check(Rules.matches("8.1.0",27),"patch release");check(Rules.matches("9",28),"correct label");
  check(!Rules.matches("9.1",28),"nonexistent Android 9.1 must be flagged even with API 28");
  check(Rules.matches("9.0.0",28),"zero suffix Android 9");
  check(!Rules.matches("10.1",29),"nonexistent minor release");
  check(Rules.matches("12.1",32),"Android 12L label");
  check(Rules.matches("12L",32),"Android 12L name");
  check(!Rules.matches(null,27),"missing release label");
  check(Rules.version(14).equals("4.0"),"minimum mapping");check(Rules.version(27).equals("8.1"),"8.1 mapping");
  check(Rules.inferredVersion(27,false).equals("Android 8.1"),"inferred from API rather than label");
  check(Rules.inferredVersion(28,true).equals("无法确定"),"conflict must not claim real version");
  check(Rules.inferredVersion(100,false).equals("无法确定"),"unknown API");
  check(Rules.recommend(100,false,true)[3].isEmpty(),"unknown API must not provide download");
  check(Rules.alternative(25,false,true)[2].contains("Legacy"),"API25 fallback");
  check(Rules.alternative(26,false,true)[2].contains("Android8"),"API26 fallback");
  check(Rules.alternative(27,false,true)[2].contains("Android8"),"API27 fallback");
  check(Rules.alternative(28,false,true)[2].equals("DiPlay-0.2.12.apk"),"API28 fallback");
  for(int api=14;api<=37;api++){check(Rules.alternative(api,true,true)[3].isEmpty(),"no conflict fallback "+api);check(Rules.alternative(api,false,false)[3].isEmpty(),"no ABI fallback "+api);}
  check(Rules.alternative(99,false,true)[3].isEmpty(),"no unknown fallback");
  check(Rules.alternative(24,false,true)[3].isEmpty(),"no redundant legacy fallback");
  System.out.println("PASS: "+checks+" recommendation and version checks");
 }
}
