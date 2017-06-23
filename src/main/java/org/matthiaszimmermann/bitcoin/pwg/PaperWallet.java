package org.matthiaszimmermann.bitcoin.pwg;

import java.io.File;
import java.io.IOException;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException;
import org.json.JSONObject;

public class PaperWallet {

	public static final int MNEMONIC_LENGTH_DEFAULT = 12;
	public static final String WALLET_OK = "OK";
	public static final String WALLET_ERROR = "ERROR";

	private String passPhrase;
	private String pathToFile;
	private Wallet wallet = null;

	public PaperWallet(String passPhrase) {
		this(passPhrase, getPathToFileDefault());
	}


	public PaperWallet(String passPhrase, String pathToFile) {
		this.passPhrase = setPassPhrase(passPhrase);
		this.pathToFile = setPathToFile(pathToFile);

		try {
			wallet = WalletFactory.getInstance().newWallet(MNEMONIC_LENGTH_DEFAULT, passPhrase, 1);
		}
		catch (Exception e) {
			throw new RuntimeException(String.format("%s Failed to create account: %s", WALLET_ERROR, e.getMessage()));
		}
	}
	
	public PaperWallet(File sourceFile, String passPhrase) throws Exception  {
		
		// check if provided file exists
		if(!sourceFile.exists() || sourceFile.isDirectory()) { 
			throw new IOException(String.format("%s file does not exist (or path is a directory)", WALLET_ERROR));
		}

		// read/decrypt wallet from file
		try {
			wallet = WalletFactory.getInstance().restoreWalletFromJSON(sourceFile.getAbsolutePath(), passPhrase);
		} 
		catch (MnemonicLengthException e) {
			e.printStackTrace();
			throw new RuntimeException(String.format("%s mnemonic length exception", WALLET_ERROR));
		} 
		catch (DecoderException e) {
			e.printStackTrace();
			throw new RuntimeException(String.format("%s decoding exception", WALLET_ERROR));
		}
	}
	
	public JSONObject getJson() {
		if(wallet == null) {
			return null;
		}
		
		return wallet.toJSON();
	}
	
	public static String checkWalletFileStatus(File sourceFile, String passPhrase) {
		try {
			new PaperWallet(sourceFile, passPhrase);
			return WALLET_OK;
		} 
		catch (Exception e) {
			return e.getMessage();
		}
	}
	
	public Wallet getWallet() {
		return wallet;
	}
	
	public String getAddress() {
		return wallet.getAccount(0).getReceive().getAddressAt(0).getAddressString();
	}

	public String getMnemonic() {
		if(wallet == null) {
			return null;
		}
		
		return wallet.getMnemonic();
	}

	public String getPassPhrase() {
		if(wallet == null) {
			return null;
		}
		
		return wallet.getPassphrase();
	}

	public String getPathToFile() {
		return pathToFile;
	}

	public String getFileName() {
		return String.format("%s.json", getBaseName());
	}
	
	public String getBaseName() {
		return getAddress();
	}
	
	public static String getPathToFileDefault() {
		return System.getProperty("user.home");
	}

	public String getFileContent() throws Exception {
		return wallet.toJSON().toString(4);
	}

	public File getFile() {
		return new File(pathToFile, getFileName());
	}

	private String setPassPhrase(String passPhrase) {
		this.passPhrase = passPhrase == null ? "" : passPhrase;
		return this.passPhrase;
	}

	private String setPathToFile(String pathToFile) {
		if(pathToFile == null || pathToFile.isEmpty()) {
			return getPathToFileDefault();
		}

		return pathToFile;
	}
}
