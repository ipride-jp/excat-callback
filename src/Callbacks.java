import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created on 2005/12/11
 *  for excat(jdk 5)
 */

public class Callbacks {

    public native static String dumpstack(Object thread, Class exceptionCls, String obj,String obj2);
	public native static String dumpstackForMethod(Object thread, String url);
	public native static void registerInstance(Object thread, Object obj, String className, String url);

	private static Notifier notifier = new DumpMailer();
	private static final Object myLock = new Object();
	private static List exceptionList = new ArrayList();
	private static int inMyself = 0;

	static byte[] classData;

	public static void setNotifierSetting(HashMap setting) throws Exception {
		notifier.setSetting(setting);
	}

	public static void callback(Object obj) {
		String exceptionName = null;
		String parentexceptionName = null;
		String monitorClassName = null;
		if (obj != null && obj instanceof Throwable) {
			Throwable th = (Throwable) obj;
			exceptionName = th.getClass().getName();

			boolean found = false;
			synchronized (myLock) {
				try {
					parentexceptionName = exceptionName;
					Class parentClass = th.getClass();
					do {
						for (int i = 0; i < exceptionList.size(); i++) {
							monitorClassName = (String) exceptionList.get(i);
							if (parentexceptionName.equals(monitorClassName)) {
								found = true;
								break;
							}
						}
						if (found)
						{
							break;
						}
						//get parent class	name
						parentClass = parentClass.getSuperclass();
						parentexceptionName = parentClass.getName();
					}while (!(parentexceptionName.equals("java.lang.Object")));
					
					if (!found) {
						return;
					}
					inMyself++;
					
					//call jni function to check if to dump
					if (inMyself == 1) {
						dumpstack(Thread.currentThread(),th.getClass(), exceptionName,monitorClassName);
					}
					inMyself--;
				}catch(Throwable thx){
					//thx.printStackTrace();
					inMyself--;
				}
			}
		}
	}

	/**
	 * �Ď�������O�����������ꍇ�A���[�����M���s��
	 * @param exceptionName ��O��
	 * @param outputFilePath�@�_���v�t�@�C�����ۑ������p�X
	 * @param attachFile�@�_���v�t�@�C����Y�t���邩�ǂ���
	 * @return ���[�����M�����������ꍇ�Atrue�����^�[������A���s
	 * �����ꍇ�Afalse�����^�[������
	 */
	public static boolean sendMailForException(String exceptionName,String outputFilePath,boolean attachFile)
	{
  		HashMap data = new HashMap();
		data.put("ExceptionName", exceptionName);
		data.put("OutputFilePath", outputFilePath);
		data.put("AttachFile", new Boolean(attachFile));
		data.put("BodyTemplateFileName", "Exception.text");
		return notifier.send(data); 	
    }

	public static boolean isSuperClass(Class clz, String superName) 
	{
		if (superName.equals(clz.getName())) {
			return true;
		}
		if ("java.lang.Object".equals(clz.getName())) {
			return false;
		}
		return isSuperClass(clz.getSuperclass(), superName);
	}

	/**
	 * �Ď������O�����X�g�ɐݒ肷��
	 * @param exceptions
	 */
	public static void setMonitorExceptions(String[] exceptions) 
	{
		//dumpstack(Thread.currentThread(), null);
		synchronized (myLock) {
			exceptionList.clear();
			for (int i = 0; i < exceptions.length; i++) {
				exceptionList.add(exceptions[i]);
			}
		}
	}

	/**
	 * �w�肷��N���X���[�h�ƃN���X���ɂ���āA�N���X��{��
	 * @param  ClassLoader
	 * @param  className
	 * @return Class object
	 *         if class not found, return null
	 */
	public static Class getClassObject(ClassLoader cl, String className) 
	{
		if (className == null)
			return null;

		//if cl is null�Aget system class loader for instead
		if (cl == null) {
			cl = ClassLoader.getSystemClassLoader();
			if (cl == null)
				return null;
		}

		String name = className.replace('.', '/');
		name += ".class";

		URL path = cl.getResource(name);
		if (path == null) {
			return null;
		}

		try {
			Class clazz = cl.loadClass(className.replace('/', '.'));
			return clazz;
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	/**
	 * �w�肷��N���X���[�h�ƃN���X���ɂ���āA�N���X�t�@�C���̃t���p�X���擾����
	 * @param  ClassLoader
	 * @param  className
	 * @return class file' full path.
	 *         if class file not found, return null
	 */
	public static String getClassUrl(ClassLoader cl, String className) {

		if (className == null)
			return null;

		//if cl is null�Aget system class loader for instead
		if (cl == null) {
			cl = ClassLoader.getSystemClassLoader();
			if (cl == null)
				return null;
		}

		String name = className.replace('.', '/');
		name += ".class";

		URL path = cl.getResource(name);
		if (path == null) {
			return null;
		}
		try {
			String urls = URLDecoder.decode(path.getPath(), "UTF-8");
			return urls;
		}catch(UnsupportedEncodingException e)
		{ 
			return path.getPath();
		}
	}

	/**
	 * �w�肷��N���X���[�h�ƃN���X���ɂ���āA�N���X�t�@�C���̓��e���擾����
	 * @param  ClassLoader
	 * @param  className
	 * @return true  : Success to get class data
	 *         false : Failed to get class data
	 */
	public static int getClassData(ClassLoader cl, String className)
	{
		if (className == null)
			return 0;

		//if cl is null�Aget system class loader for instead
		if (cl == null) {
			cl = ClassLoader.getSystemClassLoader();
			if (cl == null)
				return 0;
		}

		String name = className.replace('.', '/');
		name += ".class";
		InputStream is = cl.getResourceAsStream(name);
		if (is == null) {
			return 0;
		}

		int classDataLen = 0;
		try {
			DataInputStream dis = new DataInputStream(is);
			classDataLen = is.available();
			classData = new byte[classDataLen];
			dis.readFully(classData);
			dis.close();
			is.close();
		} catch (IOException e) {
			return 0;
		}
		return classDataLen;
	}

	public static void callback_for_method(String url)
	{
		synchronized(myLock){
			inMyself++;
			dumpstackForMethod(Thread.currentThread(), url);
			inMyself--;
		}
	}

	/**
	* �Ď�����Method�̃_���v���������ꍇ�A���[�����M���s��
	* @param outputFilePath�@�_���v�t�@�C�����ۑ������p�X
	* @param attachFile�@�_���v�t�@�C����Y�t���邩�ǂ���
	* @return ���[�����M�����������ꍇ�Atrue�����^�[������A���s
	* �����ꍇ�Afalse�����^�[������
	*/
    public static boolean sendMailForMethod(String outputFilePath,
    		boolean attachFile)
	{
		if (outputFilePath == null) {
			return false;
		}	
		
  		HashMap data = new HashMap();
		String separator = File.separator;
		if ("\\".equals(separator)) {
			separator = "\\\\";
		}
		String[] parts = outputFilePath.split(separator);
		//�N���X���ƃ��\�b�h��
		if (parts.length >= 3) {
			String className = parts[parts.length - 3];
			className = className.replace('#', '$');
			data.put("ClassName", className);
			
			String methodName = parts[parts.length - 2];
			methodName = methodName.replace('#', '$');
			data.put("MethodName", methodName);
		}
		
		data.put("OutputFilePath", outputFilePath);
		data.put("BodyTemplateFileName", "Method.text");  		
		data.put("AttachFile", new Boolean(attachFile));
		return notifier.send(data); 	
    }
    
	public static String formatDateObject(Date dateObj, String formatStr)
	{
		SimpleDateFormat df = new SimpleDateFormat(formatStr);
		return df.format(dateObj);
	}

	public static void callback_for_instance(Object obj, String className, String classUrl) {
		synchronized(myLock){
			registerInstance(Thread.currentThread(), obj, className, classUrl);
		}
	}
}
