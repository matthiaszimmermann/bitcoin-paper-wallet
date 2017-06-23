package org.matthiaszimmermann.bitcoin.pwg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ApplicationTest extends BaseTest {

	public static final String WALLET_MNEMONIC = "angle end trade shiver title learn shove top wonder exotic lamp puppy";
	public static final String WALLET_JSON_PASS_PHRASE = "test pass phrase";
	public static final String WALLET_JSON_OK = "{\"seed\":\"dQYSjX6+aGmzn7AhDZhNJnIx8rvjfttgCQL0khMums4=\",\"accounts\":[{\"chains\":[{\"path\":\"M/44H/0H/0H/0\",\"addresses\":[{\"path\":\"M/44H/0H/0H/0/0\",\"address\":\"1FteBgh6KQ3Bnv4SSx8r2oLE198uExs5Te\"},{\"path\":\"M/44H/0H/0H/0/1\",\"address\":\"1JcGxWyYoqnU9DHGnt8RsKYyz738bKvydU\"}]},{\"path\":\"M/44H/0H/0H/1\",\"addresses\":[{\"path\":\"M/44H/0H/0H/1/0\",\"address\":\"1JcUxdTcE5UcCCFAv2QwoVPqWNFxo6VB57\"},{\"path\":\"M/44H/0H/0H/1/1\",\"address\":\"18gXGiQ2dfCVTFioJp35bTANjzmVczyyHA\"}]}]}],\"version\":\"1.0\",\"iv\":\"7RFrnKNxd+xIUlFYK05cMw==\"}"; 
	public static final String WALLET_JSON_CORRUPT_1 = "{\"seed\":\"dQYSjX6+aGmzn7AhDZhNJnIx8rvjfttgCQL0khMums4=\",\"accounts\":[{\"chains\":[{\"path\":\"M/44H/0H/0H/0\",\"addresses\":[{\"path\":\"M/44H/0H/0H/0/0\",\"address\":\"1FteBgh6KQ3Bnv4SSx8r2oLE198uExs5Te\"},{\"path\":\"M/44H/0H/0H/0/1\",\"address\":\"1JcGxWyYoqnU9DHGnt8RsKYyz738bKvydU\"}]},{\"path\":\"M/44H/0H/0H/1\",\"addresses\":[{\"path\":\"M/44H/0H/0H/1/0\",\"address\":\"1JcUxdTcE5UcCCFAv2QwoVPqWNFxo6VB57\"},{\"path\":\"M/44H/0H/0H/1/1\",\"address\":\"18gXGiQ2dfCVTFioJp35bTANjzmVczyyHA\"}]}]}],\"version\":\"0.1\",\"iv\":\"7RFrnKNxd+xIUlFYK05cMw==\"}";
	public static final String WALLET_JSON_CORRUPT_2 = "{\"accounts\":[{\"chains\":[{\"path\":\"M/44H/0H/0H/0\",\"addresses\":[{\"path\":\"M/44H/0H/0H/0/0\",\"address\":\"1FteBgh6KQ3Bnv4SSx8r2oLE198uExs5Te\"},{\"path\":\"M/44H/0H/0H/0/1\",\"address\":\"1JcGxWyYoqnU9DHGnt8RsKYyz738bKvydU\"}]},{\"path\":\"M/44H/0H/0H/1\",\"addresses\":[{\"path\":\"M/44H/0H/0H/1/0\",\"address\":\"1JcUxdTcE5UcCCFAv2QwoVPqWNFxo6VB57\"},{\"path\":\"M/44H/0H/0H/1/1\",\"address\":\"18gXGiQ2dfCVTFioJp35bTANjzmVczyyHA\"}]}]}],\"version\":\"1.0\",\"iv\":\"7RFrnKNxd+xIUlFYK05cMw==\"}";;

	private static List<File> tmpFile = new ArrayList<>();
	private static String tmpFilePath;
	private static boolean setupFailed = false;

	@BeforeClass
	public static void setUp() {
		try {
			File f = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
			String p = f.getAbsolutePath();
			tmpFilePath = p.substring(0, p.lastIndexOf(File.separator));
			tmpFile.add(f);

		} 
		catch (IOException e) {
			setupFailed = true;
		}
	}

	@AfterClass
	public static void tearDown() {
		for(File f: tmpFile) {
			System.out.print("deleting temp file " + f.getAbsolutePath() + " ... ");
			if(f != null && f.exists()) {
				f.delete();
				System.out.println(" done");
			}
			else {
				System.out.println(" no such file found");
			}
		}
	}

	@Test
	public void verifySetup() {
		Assert.assertTrue("failed to get path to temp directory", !setupFailed);
	}
	
	@Test
	public void createWalletHappyCase() {
		if(setupFailed) {
			return;
		}

		String passPhrase = "test pass phrase";
		String [] args = new String [] { Application.SWITCH_DIRECTORY, tmpFilePath, Application.SWITCH_PASS_PHRASE, passPhrase};
		Application app = new Application();
		String message = app.run(args);
		Wallet wallet = app.getWallet();
		boolean isOkMessage = message.startsWith(Application.CREATE_OK);

		Assert.assertTrue(String.format("failed to write paper wallet to directory %s: expected message '%s ...', actual message: '%s'", tmpFilePath, Application.CREATE_OK, message), isOkMessage);

		log("--------------------------------------");
		log("wallet mnemonic: %s", wallet.getMnemonic());
		log("wallet receive address: %s", wallet.getAddress().getAddressString());
		log("wallet seed (hex): %s", wallet.getSeedHex());
		log("wallet seed bytes: %s", bytesToString(wallet.getSeed()));
		log("wallet pass phrase: %s", wallet.getPassphrase());
		log("--------------------------------------");
		
		if(isOkMessage) {
			File jsonFile = new File(okMessageToJsonFileName(message));
			File htmlFile = deriveFile(jsonFile, Application.EXT_HTML);
			File pngFile = deriveFile(jsonFile, Application.EXT_PNG);

			Assert.assertTrue("failed to create json file " + jsonFile.getAbsolutePath(), jsonFile.exists());
			Assert.assertTrue("failed to create html file " + htmlFile.getAbsolutePath(), htmlFile.exists());
			Assert.assertTrue("failed to create png file " + pngFile.getAbsolutePath(), pngFile.exists());

			tmpFile.add(jsonFile);
			tmpFile.add(htmlFile);
			tmpFile.add(pngFile);
		}
	}

	private String bytesToString(byte[] seed) {
		StringBuffer sb = new StringBuffer("[");
		
		for(byte b: seed) {
			sb.append(b);
			sb.append(", ");
		}
		
		return sb.substring(0, sb.length() - 2) + "]";
	}

	@Test
	public void verifyWalletHappyCase() {
		if(setupFailed) {
			return;
		}

		String jsonFile = String.format("%s%s%s", tmpFilePath, File.separator, "wallet_verify_ok.json");
		FileUtility.saveToFile(WALLET_JSON_OK, jsonFile);

		String [] args = new String [] { Application.SWITCH_DIRECTORY, tmpFilePath, Application.SWITCH_PASS_PHRASE, WALLET_JSON_PASS_PHRASE, Application.SWITCH_VERIFY, jsonFile};
		Application app = new Application();
		String message = app.run(args);
		boolean isOkMessage = message.startsWith(Application.VERIFY_OK);

		Assert.assertTrue(String.format("failed to verify paper wallet %s: expected message '%s ...', actual message: '%s'", jsonFile, Application.VERIFY_OK, message), isOkMessage);
		tmpFile.add(new File(jsonFile));
	}

	@Test
	public void verifyWalletWithMnemonic() {
		if(setupFailed) {
			return;
		}

		String jsonFile = String.format("%s%s%s", tmpFilePath, File.separator, "wallet_verify_ok.json");
		FileUtility.saveToFile(WALLET_JSON_OK, jsonFile);

		String [] args = new String [] { Application.SWITCH_DIRECTORY, tmpFilePath, Application.SWITCH_PASS_PHRASE, WALLET_JSON_PASS_PHRASE, Application.SWITCH_VERIFY, jsonFile};
		Application app = new Application();
		String message = app.run(args);
		boolean isOkMessage = message.startsWith(Application.VERIFY_OK);

		Assert.assertTrue(String.format("failed to verify paper wallet %s: expected message '%s ...', actual message: '%s'", jsonFile, Application.VERIFY_OK, message), isOkMessage);
		tmpFile.add(new File(jsonFile));
	}

	@Test
	public void createAndVerifyWalletHappyCase() {
		if(setupFailed) {
			return;
		}

		// create wallet file
		String passPhrase = "hi";
		String [] args = new String [] { Application.SWITCH_DIRECTORY, tmpFilePath, Application.SWITCH_PASS_PHRASE, passPhrase};
		Application app = new Application();
		String message = app.run(args);
		boolean isOkMessage = message.startsWith(Application.CREATE_OK);

		Assert.assertTrue(String.format("failed to write paper wallet to directory %s: expected message '%s ...', actual message: '%s'", tmpFilePath, Application.CREATE_OK, message), isOkMessage);
		updateTempFiles(message);

		// verify wallet file
		String jsonFile = okMessageToJsonFileName(message);
		args = new String [] { Application.SWITCH_DIRECTORY, tmpFilePath, Application.SWITCH_PASS_PHRASE, passPhrase, Application.SWITCH_VERIFY, jsonFile};
		app = new Application();
		message = app.run(args);
		isOkMessage = message.startsWith(Application.VERIFY_OK);

		Assert.assertTrue(String.format("failed to verify paper wallet %s: expected message '%s ...', actual message: '%s'", jsonFile, Application.VERIFY_OK, message), isOkMessage);
	}

	@Test
	public void verifyWalletFileMissing() {
		if(setupFailed) {
			return;
		}

		String jsonFile = String.format("%s%s%s", tmpFilePath, File.separator, "wallet_verify_missing_file.json");
		String badPassPhrase = WALLET_JSON_PASS_PHRASE;
		String [] args = new String [] { Application.SWITCH_PASS_PHRASE, badPassPhrase, Application.SWITCH_VERIFY, jsonFile};
		Application app = new Application();
		String message = app.run(args);
		boolean isErrorMessage = message.startsWith(Application.VERIFY_ERROR);

		Assert.assertTrue(String.format("failed to force verification error for inexistent file. expected message '%s ...', actual message: '%s'", Application.VERIFY_ERROR, message), isErrorMessage);
	}

	@Test
	public void verifyWalletBadPassPhrase() {
		if(setupFailed) {
			return;
		}

		String jsonFile = String.format("%s%s%s", tmpFilePath, File.separator, "wallet_verify_bad_pass_phrase.json");
		FileUtility.saveToFile(WALLET_JSON_OK, jsonFile);

		String badPassPhrase = WALLET_JSON_PASS_PHRASE + " bad";
		String [] args = new String [] { Application.SWITCH_PASS_PHRASE, badPassPhrase, Application.SWITCH_VERIFY, jsonFile};
		Application app = new Application();
		String message = app.run(args);
		boolean isErrorMessage = message.startsWith(Application.VERIFY_ERROR);

		Assert.assertTrue(String.format("failed to force pass phrase verification error. expected message '%s ...', actual message: '%s'", Application.VERIFY_ERROR, message), isErrorMessage);
		tmpFile.add(new File(jsonFile));
	}

	@Test
	public void verifyWalletEmptyFile() {
		if(setupFailed) {
			return;
		}

		String jsonFile = String.format("%s%s%s", tmpFilePath, File.separator, "wallet_verify_empty.json");
		FileUtility.saveToFile("", jsonFile);

		String passPhrase = WALLET_JSON_PASS_PHRASE;
		String [] args = new String [] { Application.SWITCH_PASS_PHRASE, passPhrase, Application.SWITCH_VERIFY, jsonFile};
		Application app = new Application();
		String message = app.run(args);
		boolean isErrorMessage = message.startsWith(Application.VERIFY_ERROR);

		Assert.assertTrue(String.format("failed to force empty file verification error. expected message '%s ...', actual message: '%s'", Application.VERIFY_ERROR, message), isErrorMessage);
		tmpFile.add(new File(jsonFile));
	}

	@Test
	public void verifyWalletCorruptFileTruncated() {
		if(setupFailed) {
			return;
		}

		String jsonFile = String.format("%s%s%s", tmpFilePath, File.separator, "wallet_truncated.json");
		FileUtility.saveToFile(WALLET_JSON_OK.substring(0, WALLET_JSON_OK.length() - 23), jsonFile);

		String badPassPhrase = WALLET_JSON_PASS_PHRASE;
		String [] args = new String [] { Application.SWITCH_PASS_PHRASE, badPassPhrase, Application.SWITCH_VERIFY, jsonFile};
		Application app = new Application();
		String message = app.run(args);
		boolean isErrorMessage = message.startsWith(Application.VERIFY_ERROR);

		Assert.assertTrue(String.format("failed to force truncated file verification error. expected message '%s ...', actual message: '%s'", Application.VERIFY_ERROR, message), isErrorMessage);
		tmpFile.add(new File(jsonFile));
	}

	@Test
	public void verifyWalletBadVersion() {
		if(setupFailed) {
			return;
		}

		String jsonFile = String.format("%s%s%s", tmpFilePath, File.separator, "wallet_bad_version.json");
		FileUtility.saveToFile(WALLET_JSON_CORRUPT_1, jsonFile);

		String passPhrase = WALLET_JSON_PASS_PHRASE;
		String [] args = new String [] { Application.SWITCH_PASS_PHRASE, passPhrase, Application.SWITCH_VERIFY, jsonFile};
		Application app = new Application();
		String message = app.run(args);
		boolean isErrorMessage = message.startsWith(Application.VERIFY_ERROR);

		Assert.assertTrue(String.format("failed to force bad version verification error. expected message '%s ...', actual message: '%s'", Application.VERIFY_ERROR, message), isErrorMessage);
		tmpFile.add(new File(jsonFile));
	}

	@Test
	public void verifyWalletMissingElement() {
		if(setupFailed) {
			return;
		}

		String jsonFile = String.format("%s%s%s", tmpFilePath, File.separator, "wallet_missing_elements.json");
		FileUtility.saveToFile(WALLET_JSON_CORRUPT_2, jsonFile);

		String badPassPhrase = WALLET_JSON_PASS_PHRASE;
		String [] args = new String [] { Application.SWITCH_PASS_PHRASE, badPassPhrase, Application.SWITCH_VERIFY, jsonFile};
		Application app = new Application();
		String message = app.run(args);
		boolean isErrorMessage = message.startsWith(Application.VERIFY_ERROR);

		Assert.assertTrue(String.format("failed to force missing elements verification error. expected message '%s ...', actual message: '%s'", Application.VERIFY_ERROR, message), isErrorMessage);
		tmpFile.add(new File(jsonFile));
	}

	private void updateTempFiles(String message) {
		if(message == null || !message.startsWith(Application.CREATE_OK)) {
			return;
		}

		File jsonFile = new File(okMessageToJsonFileName(message));
		File htmlFile = deriveFile(jsonFile, Application.EXT_HTML);
		File pngFile = deriveFile(jsonFile, Application.EXT_PNG);

		tmpFile.add(jsonFile);
		tmpFile.add(htmlFile);
		tmpFile.add(pngFile);
	}

	private String okMessageToJsonFileName(String message) {
		return message.substring(Application.CREATE_OK.length() + 1);
	}

	private File deriveFile(File file, String newExtension) {
		String baseName = getBaseName(file.getAbsolutePath());
		return new File(String.format("%s.%s", baseName, newExtension));
	}

	private String getBaseName(String jsonName) {
		int pos = jsonName.lastIndexOf(".");
		return pos >= 0? jsonName.substring(0, pos) : jsonName; 
	}
}
