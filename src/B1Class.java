import java.util.LinkedList;
import java.util.List;

/**
 * @(#)B1Class.java
 * 
 * Copyright 2005 NTT DATA Corporation
 * 
 */

/**
 *<p></p>
 *
 *@author kyo
 *@version 1.0
 */
public class B1Class {

   int m_b1 = 1;

   List list = null;
   
   public B1Class(){
	   list = new LinkedList();
	   list.add("abc");
	   list.add("efg");
	   list.add("123");
   }
}
