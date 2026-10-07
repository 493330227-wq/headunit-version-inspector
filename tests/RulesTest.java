package cn.headunit.inspector;
public class RulesTest {
 static int checks=0;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  check(Rules.recommend(18,false,true)[2].isEmpty(),"below minimum");
  for(int api=19;api<=25;api++)check(Rules.recommend(api,false,true)[2].equals("DiPlay-Legacy-Android-v0.2.7.apk"),"legacy "+api);
  for(int api=26;api<=27;api++)check(Rules.recommend(api,false,true)[2].equals("DiPlay-Android8-complete-debug.apk"),"krunk "+api);
  for(int api=28;api<=36;api++)check(Rules.recommend(api,false,true)[2].equals("DiPlay-0.2.12.apk"),"official "+api);
  for(int api=14;api<=36;api++){check(Rules.recommend(api,true,true)[2].isEmpty(),"hold conflict "+api);check(Rules.recommend(api,false,false)[2].isEmpty(),"hold ABI "+api);}
  check(!Rules.matches("9.1",27),"spoofed label");check(Rules.matches("8.1.0",27),"patch release");check(Rules.matches("9",28),"correct label");
  check(Rules.version(14).equals("4.0"),"minimum mapping");check(Rules.version(27).equals("8.1"),"8.1 mapping");
  check(Rules.inferredVersion(27,false).equals("Android 8.1"),"inferred from API rather than label");
  check(Rules.inferredVersion(28,true).equals("无法确定"),"conflict must not claim real version");
  check(Rules.inferredVersion(100,false).equals("无法确定"),"unknown API");
  check(Rules.recommend(100,false,true)[3].isEmpty(),"unknown API must not provide download");
  System.out.println("PASS: "+checks+" recommendation and version checks");
 }
}
