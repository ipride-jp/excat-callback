/**
 * @(#)A1Class.java
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
public class A1Class {

	private A11Class m_a11 = new A11Class();
	private A12Class m_a12 = new A12Class();
	
	public BClass getBClass(){
		return m_a11.getBClass();
	}
}
