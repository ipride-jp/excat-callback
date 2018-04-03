import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class DumpMailer implements Notifier {
	private static final String SETTING_PASSWORD = "Password";
	private static final String SETTING_ACCOUNT = "Account";
	private static final String SETTING_IS_AUTH = "IsAuth";
	private static final String SETTING_SMTP_SERVER_PORT = "SmtpServerPort";
	private static final String SETTING_SMTP_SERVER = "SmtpServer";
	private static final String SETTING_SUBJECT = "Subject";
	private static final String SETTING_BODY_TEMPLATE_FOLDER_PATH = "BodyTemplateFolderPath";
	private static final String SETTING_TO = "To";
	private static final String SETTING_FROM = "From";
	private static final String SETTING_FROM_NAME = "FromName";

	private static final String[] VAR_NAMES = { 
		"ExceptionName",
		"ClassName",
		"MethodName",
		"OutputFilePath"
		};

	private HashMap bodyTemplateCache;

	private boolean isAuth;
	private String account;
	private String password;
	private String smtpServer;
	private int smtpServerPort;
	private InternetAddress from;
	private String fromName;
	private InternetAddress[] to;
	private String subject;
	private String bodyTemplateFolderPath;

	public DumpMailer() {
		this.init();
	}

	public boolean send(HashMap data) {
		if (this.smtpServer == null) {
			return false;
		}

		String outputFilePath = null;
		if (data.get("OutputFilePath") != null) {
			outputFilePath = (String) data.get("OutputFilePath");
		}
		// ダンプしていない場合、
		if (outputFilePath == null || "".equals(outputFilePath)) {
			return false;
		}

		try {
			File zipFile = null;
			Properties props = System.getProperties();

			// SMTPサーバーのアドレスを指定
			props.put("mail.smtp.host", this.smtpServer);
			if (smtpServerPort != 0) {
				props.put("mail.smtp.port", String.valueOf(smtpServerPort));
			}
			props.put("mail.smtp.auth", String.valueOf(this.isAuth));
			Session session = Session.getDefaultInstance(props, null);

			// 送信メッセージを作成
			MimeMessage mimeMessage = new MimeMessage(session);

			// 送信元メールアドレスと送信者名を指定
			this.from.setPersonal(this.fromName, "UTF-8");
			mimeMessage.setFrom(this.from);

			// 送信先メールアドレスを指定
			mimeMessage.setRecipients(Message.RecipientType.TO, this.to);

			// メールのタイトルを指定
			mimeMessage.setSubject(this.subject, "UTF-8");

			// 送信日付を指定
			mimeMessage.setSentDate(new Date());

			// ボディテンプレートをロードする
			String bodyTemplateFileName = (String) data
					.get("BodyTemplateFileName");
			String bodyTemplate = this.loadBodyTemplate(
					 bodyTemplateFileName);
			String body = this.fillVariable(bodyTemplate, data);

			// メールの内容を指定
			Boolean attachFile =(Boolean)data.get("AttachFile");
			if (attachFile.booleanValue()) {
				MimeMultipart content = new MimeMultipart();
				mimeMessage.setContent(content);
				MimeBodyPart text = new MimeBodyPart();
				text.setContent(body, "text/plain; charset=UTF-8");
				text.setHeader("Content-Transfer-Encoding", "7bit");
				content.addBodyPart(text);

				MimeBodyPart attach = new MimeBodyPart();
				zipFile = this.zipFile(outputFilePath);
				FileDataSource fds = new FileDataSource(zipFile);
				DataHandler dh = new DataHandler(fds);
				attach.setDataHandler(dh);

				int index = outputFilePath.lastIndexOf(File.separator);
				attach.setFileName(outputFilePath.substring(index + 1) + ".zip");
				content.addBodyPart(attach);
			} else {
				mimeMessage.setText(body, "UTF-8");
			}

			//送信
			if (this.isAuth) {
				Transport tp = session.getTransport("smtp");
				if (this.smtpServerPort == 0) {
					tp.connect(this.smtpServer, this.account, this.password);
				} else {
					tp.connect(this.smtpServer, this.smtpServerPort,
							this.account, this.password);
				}
				tp.sendMessage(mimeMessage, this.to);
			} else {
				Transport.send(mimeMessage);
			}
			
			//Zipファイルを削除
			if (zipFile != null) {
				zipFile.delete();
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private String fillVariable(String text, HashMap data) {
		for (int i = 0; i < VAR_NAMES.length; i++) {
			if (data.get(VAR_NAMES[i]) == null) {
				continue;
			}

			String replacement = (String) data.get(VAR_NAMES[i]);
			replacement = replacement.replaceAll("\\\\", "\\\\\\\\");
			replacement = replacement.replaceAll("\\$", "\\\\\\$");
			text = text.replaceAll("\\Q{" + VAR_NAMES[i] + "}\\E", replacement);
		}

		return text;
	}

	/**
	 * メールアドレスを解析して、InternetAddressタイプのアドレスを生成する。
	 * 
	 * @param mailAddressText
	 *            「aaa@tt.com, bbb@tt.com」ような形式のメールアドレス
	 * @return InternetAddressタイプのアドレス
	 * @throws AddressException
	 */
	InternetAddress[] parseMailAddressText(String mailAddressText)
			throws AddressException {
		if (null == mailAddressText || "" == mailAddressText) {
			return new InternetAddress[] {};
		}

		String[] addressStrs = mailAddressText.split(",");
		InternetAddress[] addresses = new InternetAddress[addressStrs.length];
		for (int i = 0; i < addresses.length; i++) {
			addresses[i] = new InternetAddress(addressStrs[i].trim());
		}

		return addresses;
	}

	String loadBodyTemplate(String bodyTemplateFileName) throws IOException {
		
		String bodyTemplateFilePath = null;
		boolean useDefaultTemplate = false;
		
		if(this.bodyTemplateFolderPath != null && 
				!"".equals(this.bodyTemplateFolderPath)){
			bodyTemplateFilePath 	= this.bodyTemplateFolderPath
			+ File.separator + bodyTemplateFileName;
			File file = new File(bodyTemplateFilePath);
			if(!file.exists()){
				bodyTemplateFilePath = bodyTemplateFileName;
				useDefaultTemplate = true;
			}
		}else{
			bodyTemplateFilePath = bodyTemplateFileName;
			useDefaultTemplate = true;
		}
	
		if (this.bodyTemplateCache.containsKey(bodyTemplateFilePath)) {
			return (String) this.bodyTemplateCache.get(bodyTemplateFilePath);
		}

		BufferedReader reader = null;
		if(useDefaultTemplate){
			//read from file 
			InputStream is = ClassLoader.getSystemResourceAsStream(bodyTemplateFilePath);
			reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));		
		}else{
			//read from file 
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(
					bodyTemplateFilePath)), "UTF-8"));
		}

		StringBuffer buffer = new StringBuffer();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line + "\n");
		}
		reader.close();

		String body = buffer.toString();
		this.bodyTemplateCache.put(bodyTemplateFilePath, body);
		return body;
	}

	public void setSetting(HashMap setting) throws Exception {
		this.init();

		//メールアドレステキストを解析する。
		this.from = this.parseMailAddressText((String) setting
				.get(SETTING_FROM))[0];
		this.fromName = (String) setting.get(SETTING_FROM_NAME);
		this.to = this.parseMailAddressText((String) setting.get(SETTING_TO));

		//ボディの内容をロードする。
		this.bodyTemplateFolderPath = (String) setting
				.get(SETTING_BODY_TEMPLATE_FOLDER_PATH);

		this.subject = (String) setting.get(SETTING_SUBJECT);
		this.smtpServer = (String) setting.get(SETTING_SMTP_SERVER);
		String smtpServerPortStr = (String) setting
				.get(SETTING_SMTP_SERVER_PORT);
		if (smtpServerPortStr != null && !"".equals(smtpServerPortStr)) {
			this.smtpServerPort = Integer.parseInt(smtpServerPortStr);
		}
		this.isAuth = "true".equals(setting.get(SETTING_IS_AUTH));
		this.account = (String) setting.get(SETTING_ACCOUNT);
		this.password = (String) setting.get(SETTING_PASSWORD);
	}

	private void init() {
		this.isAuth = false;
		this.account = null;
		this.password = null;
		this.smtpServer = null;
		this.smtpServerPort = 0;
		this.from = null;
		this.fromName = null;
		this.to = null;
		this.subject = null;
		this.bodyTemplateFolderPath = null;
		this.bodyTemplateCache = new HashMap();
	}
	
	private File zipFile(String filePath) throws IOException {
		File zipFile = File.createTempFile("dump", null);
		
        ZipOutputStream zipOutputStream = 
        	new ZipOutputStream(new BufferedOutputStream(
        			new FileOutputStream(zipFile)));

        byte data[] = new byte[2048];
        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(filePath));
		int index = filePath.lastIndexOf(File.separator);
        ZipEntry entry = new ZipEntry(filePath.substring(index + 1));
        zipOutputStream.putNextEntry(entry);
        
        int count;
        while ((count = inputStream.read(data, 0, 2048)) != -1) {
        	zipOutputStream.write(data, 0, count);
        }
        inputStream.close();
        zipOutputStream.close();
        
		return zipFile;
	}
}