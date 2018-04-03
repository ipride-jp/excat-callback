/**
 * @(#)AClass.java
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
public class AClass {


	private A1Class m_a1 = new A1Class();
	private A2Class m_a2 = new A2Class();;

	public BClass getBClass(){
		return m_a1.getBClass();
	}	
}
