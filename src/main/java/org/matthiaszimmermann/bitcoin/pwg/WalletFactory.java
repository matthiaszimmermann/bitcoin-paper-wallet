package org.matthiaszimmermann.bitcoin.pwg;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * WalletFactory.java : singleton class for creating/restoring/reading BIP44 HD wallet
 *
 * BIP44 extension of Bitcoinj
 *
 */
public class WalletFactory {

	public static final String BIP39_ENGLISH_SHA256 = "ad90bf3beb7b0eb7e5acd74727dc0da96e0a280a258354e7293fb7e211ac03db";
	public static final int NUM_ACCOUNTS = 1;

	private static WalletFactory instance = null;
	private static List<Wallet> wallets = null;
	private static Wallet watch_only_wallet = null;

	private static Locale locale = null;

	private WalletFactory()	{ ; }

	/**
	 * Return instance for a full wallet including seed and private keys.
	 *
	 * @return WalletFactory
	 *
	 */
	public static WalletFactory getInstance() {

		if (instance == null) {
			locale = new Locale("en", "US");
			wallets = new ArrayList<Wallet>();
			instance = new WalletFactory();
		}

		return instance;
	}

	/**
	 * Return instance for a watch only wallet. No seed, no private keys.
	 *
	 * @param  Context ctx app context
	 * @param  String[] xpub restore these accounts only
	 *
	 * @return WalletFactory
	 *
	 */
	public static WalletFactory getInstance(String[] xpub) throws AddressFormatException {

		if(instance == null) {
			locale = new Locale("en", "US");
			wallets = new ArrayList<Wallet>();
			instance = new WalletFactory();
		}

		if(watch_only_wallet == null) {
			watch_only_wallet = new Wallet(MainNetParams.get(), xpub);
		}

		return instance;
	}

	/**
	 * Set Locale. Defaults to 'en_US'
	 *
	 * @param  Locale locale to be used.
	 *
	 */
	public void setLocale(Locale loc)	{
		if(loc != null)	{
			locale = loc;
		}
		else	{
			locale = new Locale("en", "US");
		}
	}

	/**
	 * Create new wallet.
	 *
	 * @param  int nbWords number of words in menmonic
	 * @param  String passphrase optional BIP39 passphrase
	 * @param  int nbAccounts create this number of accounts
	 *
	 * @return Wallet
	 *
	 */
	public Wallet newWallet(int nbWords, String passphrase, int nbAccounts) throws IOException, MnemonicException.MnemonicLengthException   {

		Wallet hdw = null;

		if((nbWords % 3 != 0) || (nbWords < 12 || nbWords > 24)) {
			nbWords = 12;
		}

		// len == 16 (12 words), len == 24 (18 words), len == 32 (24 words)
		int len = (nbWords / 3) * 4;

		if(passphrase == null) {
			passphrase = "";
		}

		NetworkParameters params = MainNetParams.get();

		SecureRandom random = new SecureRandom();
		byte seed[] = new byte[len];
		random.nextBytes(seed);

		MnemonicCode mc = new MnemonicCode();
		hdw = new Wallet(mc, params, seed, passphrase);

		wallets.clear();
		wallets.add(hdw);

		return hdw;
	}

	/**
	 * Restore wallet.
	 *
	 * @param  String data: either BIP39 mnemonic or hex seed
	 * @param  String passphrase optional BIP39 passphrase
	 * @param  int nbAccounts create this number of accounts
	 *
	 * @return Wallet
	 *
	 */
	public Wallet restoreWallet(String data, String passphrase) throws AddressFormatException, IOException, DecoderException, MnemonicException.MnemonicLengthException, MnemonicException.MnemonicWordException, MnemonicException.MnemonicChecksumException  {

		Wallet hdw = null;

		if(passphrase == null) {
			passphrase = "";
		}

		NetworkParameters params = MainNetParams.get();

		List<String> words = null;

		MnemonicCode mc = null;
		mc = new MnemonicCode();

		byte[] seed = null;
		if(data.startsWith("xpub")) {
			String[] xpub = data.split(":");
			hdw = new Wallet(params, xpub);
		}
		else if(data.length() % 4 == 0 && !data.contains(" ")) {
			seed = AesUtility.base64ToBytes(data);
			hdw = new Wallet(mc, params, seed, passphrase);
		}
		else if(locale.toString().equals("en_US")) {
			data = data.replaceAll("[^a-z]+", " ");             // only use for BIP39 English
			words = Arrays.asList(data.trim().split("\\s+"));
			seed = mc.toEntropy(words);
			hdw = new Wallet(mc, params, seed, passphrase);
		}
		else {
			words = Arrays.asList(data.trim().split("\\s+"));
			seed = mc.toEntropy(words);
			hdw = new Wallet(mc, params, seed, passphrase);
		}

		wallets.clear();
		wallets.add(hdw);

		return hdw;
	}

	/**
	 * Get wallet for this instance.
	 *
	 * @return Wallet
	 *
	 */
	public Wallet get() throws IOException, MnemonicException.MnemonicLengthException {

		if(wallets.size() < 1) {
			wallets.clear();
			wallets.add(newWallet(12, "", 1));
		}

		return wallets.get(0);
	}

	/**
	 * Set wallet for this instance.
	 *
	 * @param  Wallet wallet
	 *
	 */
	public void set(Wallet wallet)	{

		if(wallet != null)	{
			wallets.clear();
			wallets.add(wallet);
		}

	}

	/**
	 * Return watch only wallet for this instance.
	 *
	 * @return Wallet
	 *
	 */
	public Wallet getWatchOnlyWallet()   {
		return watch_only_wallet;
	}

	/**
	 * Set watch only wallet for this instance.
	 *
	 * @param  Wallet wallet
	 *
	 */
	public void setWatchOnlyWallet(Wallet wallet)   {
		watch_only_wallet = wallet;
	}

	public Wallet restoreWalletFromJSON(String fileName, String passPhrase) throws Exception {
		JSONObject obj = readJsonWalletFile(fileName);
		return restoreWalletFromJSON(obj, passPhrase);
	}
	
	public Wallet restoreWalletFromJSON(JSONObject obj, String passPhrase) throws Exception {
		NetworkParameters params = MainNetParams.get();
		Wallet wallet = new Wallet(obj, passPhrase, params);

		log("mnemonics: " + wallet.getMnemonic());
		log("address: " + wallet.getAddress().getAddressString());
		
		wallets.clear();
		wallets.add(wallet);

		return wallet;
	}

	private JSONObject readJsonWalletFile(String fileName) throws Exception {

		// verify wallet is not empty
		String jsonString = FileUtility.readTextFile(fileName);
		if(jsonString.isEmpty()) {
			throw new JSONException("empty wallet file " + fileName);
		}

		// convert wallet file string to json object
		JSONObject node = new JSONObject(jsonString);

		// check and extract seed
		if(!node.has(Wallet.JSON_VERSION)) {
			throw new JSONException("wallet file has no version attribute " + fileName);
		}

		// check and extract seed
		if(!node.has(Wallet.JSON_SEED)) {
			throw new JSONException("wallet file has no seed attribute " + fileName);
		}

		if(!node.has(Wallet.JSON_IV)) {
			throw new JSONException("wallet file has no iv attribute " + fileName);
		}
		
		return node;
	}

	private void log(String string) {
		System.out.println(string);
	}
}
