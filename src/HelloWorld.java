
import java.io.IOException;
import java.util.HashMap;

 public class HelloWorld extends Parent {

	 //public int field1 = 24;
	 private long field3 = 22;
	 private String message;
	// private Parent parent = new Parent();
	 private String sa[] = new String[6];
	 
	 public HelloWorld() {
		 super();
		 System.out.println("init");
	 }

    public  HelloWorld(String str) {
		message = str;
		//Callbacks.callback(this);		
		System.out.println(str + " init");
    } 
  
    public static void test(int a,int b) throws Exception {
		System.out.println("test");
		//throw new NullPointerException();
		for(int i= 0; i< 1;i++){
			new MyThread().start(); 
		}
	}

    
    static class MyThread extends Thread {
    	
    	MyThread(){
    		
    	}
    	
    	public void testParams(String[] a,char b)
    	{
    		System.out.println("thread test");
    		throw new NullPointerException();
    		
    	}
    	public void run(){

    		testParams(new String[]{"32","4"},'2');
    	}
    }
    
    static void amethod(){
    	int aa = 0;
    	bmethod();
    	
    }
    
    static void bmethod(){
    	int bb = 0;
    	amethod();
    }
    
/**
 * 
 * 
 * @poseidon-object-id [Im6ccb8fc0m10952b37b23mm7da2]
 * @param args 
 */
    public static void main(String[] args) throws Exception {        
		//HelloWorld world[] = {new HelloWorld(), new HelloWorld(), new HelloWorld()};
    	
    	Object str3 = "“ú–{Œê";
		System.out.println("Hello World!");
		Object str1 = "string1";		
		char c = 'A';
		byte b = 0;
		int i = 1;
		long l = 1;
		float f = 1;
		double d = 1;
		boolean bo = true;
		short s = 1;
		String str = "French Southern & Antarctic Lands Time";
		String spaceString  = "";
		String nullString = null;
		int array1[] = {1,2,3,4};
		float array2[] = {44, 55, 66};
		String array3[] = {"\t","\b", "\n", "\f","\r","\"","\'","\\","\7"};
		HashMap map1 = new HashMap();
		map1.put("1", "abc");
		map1.put("2", "ddd");
		//Exception e1 = new Exception();
		try {

			HelloWorld ahello = 	new HelloWorld();
			byte a[] = {'0','1', '2'};
			a[0] = (byte)0xc0;
			a[1] = (byte)0xFF;
			a[2] = (byte)0xFE;
			String astring = new String(a);
			
			ahello.method1("message");
	    		
			String msg = "msg";
			HelloWorld.test(1,1);
            
			AClass aclass = new AClass();
			BClass bclass = aclass.getBClass(); 
			//amethod();
			throw new NullPointerException("error");
			//NullPointerException e1 = new NullPointerException();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("output.");
		}
		try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    } 
 }
