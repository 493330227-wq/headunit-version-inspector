package cn.headunit.inspector;
public class DemoFixtures {
 private static String q(String s) {return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";}
 public static void main(String[] args) {
  String[] labels={"标称 9.1 · 推定 8.1","Android 9 · 信息一致","Android 6 · 老车机","接口冲突 · 无法确定","Android 4.0 · 无适配包","架构未知 · 暂停推荐","未知 API · 无法确定"};
  String[] releases={"9.1","9","6.0","9.1","4.0","8.1","99"};
  int[] apis={27,28,23,28,14,27,99};
  System.out.print("[");
  for(int i=0;i<apis.length;i++) {
   boolean conflict=i==3, supported=i!=5;
   String[] rec=Rules.recommend(apis[i],conflict,supported);
   if(i>0)System.out.print(",");
   System.out.print("{\"label\":"+q(labels[i])+",\"release\":"+q(releases[i])+",\"api\":"+apis[i]+",\"conflict\":"+conflict+",\"abiSupported\":"+supported+",\"mismatch\":"+!Rules.matches(releases[i],apis[i])+",\"inferred\":"+q(Rules.inferredVersion(apis[i],conflict))+",\"title\":"+q(rec[0])+",\"reason\":"+q(rec[1])+",\"apk\":"+q(rec[2])+",\"source\":"+q(rec[3])+"}");
  }
  System.out.println("]");
 }
}
