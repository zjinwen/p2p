package com.zhangjinwen.p2p;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
  public static void info(String info){
	  SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	  System.out.println(sdf.format(new Date())+" info "+info);
  }
}
