package org.matthiaszimmermann.bitcoin.pwg;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.crypto.MnemonicException.MnemonicChecksumException;
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException;
import org.bitcoinj.crypto.MnemonicException.MnemonicWordException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Joiner;

/**
 *
 * Wallet.java : BIP44 wallet
 */
public class Wallet {
	// https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki#Examples
	public static final String BIP_44_BITCOIN_FIRST = "M/44H/0H/0H";
	public static final String BIP_44_PATH_RECEIVE = BIP_44_BITCOIN_FIRST + "/0";
	public static final String BIP_44_PATH_CHANGE = BIP_44_BITCOIN_FIRST + "/1";

	public static final String JSON_VERSION = "version";
	public static final String JSON_VERSION_VALUE = "1.0";
	
	public static final String JSON_SEED = "seed";
	public static final String JSON_IV = "iv";
	public static final String JSON_ACCOUNTS = "accounts";
	public static final String JSON_PATH = "path";
	public static final String JSON_CHAINS = "chains";
	public static final String JSON_ADDRESSES = "addresses";
	public static final String JSON_ADDRESS = "address";

	private byte[] seed = null;
	private String strPassphrase = null;
	private List<String> wordList = null;

	private DeterministicKey dkKey = null;
	private DeterministicKey dkRoot = null;

	private ArrayList<Account> accounts = null;

	private String strPath = null;

	private NetworkParameters params = null;

	@SuppressWarnings("unused")
	private Wallet() { }


	/**
	 * Constructor for wallet.
	 *
	 * @param MnemonicCode mc mnemonic code object
	 * @param NetworkParameters params
	 * @param byte[] seed seed for this wallet
	 * @param String passphrase optional BIP39 passphrase
	 * @param int nbAccounts number of accounts to create
	 *
	 */
	public Wallet(MnemonicCode mc, NetworkParameters params, byte[] seed, String passphrase) throws MnemonicException.MnemonicLengthException {
		wordList = mc.toMnemonic(seed);
		init(params, seed, passphrase);
	}
	
	public Wallet(MnemonicCode mc, NetworkParameters params, List<String> words, String passphrase) throws MnemonicLengthException, MnemonicWordException, MnemonicChecksumException  {
		wordList = words;
		init(params, mc.toEntropy(wordList), passphrase);
	}
	
	private void init(NetworkParameters params, byte[] seed, String passphrase) {
		this.params = params;
		this.seed = seed;
		strPassphrase = passphrase;
        byte[] hd_seed = MnemonicCode.toSeed(wordList, "");
		dkKey = HDKeyDerivation.createMasterPrivateKey(hd_seed);
		DeterministicKey dKey = HDKeyDerivation.deriveChildKey(dkKey, 44 | ChildNumber.HARDENED_BIT);
		dkRoot = HDKeyDerivation.deriveChildKey(dKey, ChildNumber.HARDENED_BIT);

		int nbAccounts = 1;
		accounts = new ArrayList<Account>();
		for(int i = 0; i < nbAccounts; i++) {
			accounts.add(new Account(params, dkRoot, i));
		}

		strPath = dKey.getPathAsString();
	}

	public Wallet(JSONObject jsonobj, String passPhrase, NetworkParameters params) throws Exception {

		if(!jsonobj.has(JSON_VERSION)) {
			throw new DecoderException("property 'version' missing in JSON object");
		}
		
		if(!JSON_VERSION_VALUE.equals(jsonobj.getString(JSON_VERSION))) {
			throw new DecoderException("unexpected wallet version in JSON object. expected " + JSON_VERSION_VALUE +  ", found " + jsonobj.getString(JSON_VERSION));
		}

		if(!jsonobj.has(JSON_SEED)) {
			throw new DecoderException("property 'seed' missing in JSON object");
		}

		if(!jsonobj.has(JSON_IV)) {
			throw new DecoderException("property 'iv' missing in JSON object");
		}

		if(!jsonobj.has(JSON_ACCOUNTS)) {
			throw new DecoderException("property 'accounts' missing in JSON object");
		}

		String seedString = jsonobj.getString(JSON_SEED);
		String ivString = jsonobj.getString(JSON_IV);
		JSONArray jsonAccounts = jsonobj.getJSONArray(JSON_ACCOUNTS);

		if(jsonAccounts.length() != 1) {
			throw new DecoderException("unexpected amount of 'accounts' in JSON object. expected 1, found " + jsonAccounts.length());
		}

		JSONObject jsonAccount = jsonAccounts.getJSONObject(0);

		// decrypt seed if we have a password
		if(passPhrase != null && !passPhrase.isEmpty()) {
			System.out.println("encrypted seed from wallet file: " + seedString);

			AesUtility aes = new AesUtility(passPhrase);
			seedString = aes.decrypt(seedString, ivString);
		}
		
		seed = AesUtility.base64ToBytes(seedString);
		MnemonicCode mc = new MnemonicCode();
		wordList = mc.toMnemonic(seed);
		
		init(params, seed, passPhrase);
		
		verifyAccount(accounts.get(0), jsonAccount);
	}

	private void verifyAccount(Account account, JSONObject jsonAccount) throws Exception {

		// verify that attributes exist
		if(!jsonAccount.has(JSON_CHAINS)) {
			throw new DecoderException("property 'chains' missing for account object in JSON object");
		}

		JSONArray jsonChains = jsonAccount.getJSONArray(JSON_CHAINS);

		if(jsonChains.length() != 2) {
			throw new DecoderException("unexpected amount of 'chains' in JSON object. expected 2, found " + jsonChains.length());
		}

		for(int i = 0; i < 2; i++) {
			JSONObject chain = jsonChains.getJSONObject(i);

			if(!chain.has(JSON_PATH)) {
				throw new DecoderException("property 'path' missing for chain object in JSON object");
			}

			String path = chain.getString(JSON_PATH);

			if(path.equals(BIP_44_PATH_RECEIVE)) {
				Chain receive = account.getReceive();
				verifyChain(receive, chain);
			}
			else if(path.equals(BIP_44_PATH_CHANGE)) {
				Chain change = account.getChange();
				verifyChain(change, chain);
			}
			else {
				throw new DecoderException(
						String.format("unexpected value for 'path' for chain in JSON object. expected '%s' or '%s'. found '%s'", 
								BIP_44_PATH_RECEIVE, BIP_44_PATH_CHANGE, path));
			}
		}
	}


	private void verifyChain(Chain chain, JSONObject jsonChain) throws Exception {

		String chainPath = jsonChain.getString(JSON_PATH);

		if(!jsonChain.has(JSON_ADDRESSES)) {
			throw new DecoderException("property 'addresses' missing for chain object in JSON object");
		}

		JSONArray jsonAddresses = jsonChain.getJSONArray(JSON_ADDRESSES);

		if(jsonAddresses.length() != 2) {
			throw new DecoderException("unexpected amount of 'addresses' in chain JSON object. expected 2, found " + jsonAddresses.length());
		}

		for(int i = 0; i < 2; i++) {
			JSONObject jsonAddress = jsonAddresses.getJSONObject(i);

			if(!jsonAddress.has(JSON_PATH)) {
				throw new DecoderException("property 'path' missing for chain address object in JSON object");
			}

			String jsonPath = jsonAddress.getString(JSON_PATH);

			if(!jsonPath.startsWith(chainPath)) {
				throw new DecoderException(String.format("property 'path' has unexpected start. expected '%s', found '%s", chainPath, jsonPath));
			}

			if(jsonPath.endsWith("/0")) {
				verifyAccount(chain.getAddressAt(0), jsonAddress);
			}
			else if(jsonPath.endsWith("/1")) {
				verifyAccount(chain.getAddressAt(1), jsonAddress);
			}
			else {
				throw new DecoderException(String.format("unexpected end for 'path'. expected '/0' or '/1', found '%s", 
						jsonPath.substring(jsonPath.length() - 2)));
			}

		}
	}

	private void verifyAccount(Address address, JSONObject jsonAddress) throws Exception {

		if(!jsonAddress.has(JSON_ADDRESS)) {
			throw new DecoderException("property 'address' missing for chain address object in JSON object");
		}

		String jsonPath = jsonAddress.getString(JSON_PATH);
		String jsonAddr = jsonAddress.getString(JSON_ADDRESS);

		if(!jsonAddr.equals(address.getAddressString())) {
			throw new DecoderException(String.format("JSON chain address does not match expected address. expected: %s, found %s", 
					address.getAddressString(), jsonAddr));
		}

		if(!jsonPath.equals(address.getPath())) {
			throw new DecoderException(String.format("JSON chain path does not match expected path. expected: %s, found %s", 
					address.getPath(), jsonPath));
		}

		System.out.println(String.format("address successfully verified: %s %s", jsonPath, jsonAddr));
	}


	/**
	 * Constructor for watch-only wallet initialized from submitted XPUB(s).
	 *
	 * @param NetworkParameters params
	 * @param String[] xpub array of XPUB strings
	 *
	 */
	public Wallet(NetworkParameters params, String[] xpub) throws AddressFormatException {

		this.params = params;
		accounts = new ArrayList<Account>();
		for(int i = 0; i < xpub.length; i++) {
			accounts.add(new Account(params, xpub[i], i));
		}

	}

	/**
	 * Return wallet seed as byte array.
	 *
	 * @return byte[]
	 *
	 */
	public byte[] getSeed() {
		return seed;
	}

	/**
	 * Return wallet seed as hex string.
	 *
	 * @return String
	 *
	 */
	public String getSeedHex() {
		return new String(Hex.encodeHex(seed));
	}
	
	public NetworkParameters getNetworkParameters() {
		return params;
	}

	/**
	 * Return wallet BIP39 mnemonic as string containing space separated words.
	 *
	 * @return String
	 *
	 */
	public String getMnemonic() {
		return Joiner.on(" ").join(wordList);
	}

	/**
	 * Return wallet BIP39 passphrase.
	 *
	 * @return String
	 *
	 */
	public String getPassphrase() {
		return strPassphrase;
	}

	/**
	 * Return accounts for this wallet.
	 *
	 * @return List<Account>
	 *
	 */
	public List<Account> getAccounts() {
		return accounts;
	}

	/**
	 * Return account for submitted account id.
	 *
	 * @param int accountId
	 *
	 * @return Account
	 *
	 */
	public Account getAccount(int accountId) {
		return accounts.get(accountId);
	}
	
	/**
	 * Returns first receive address
	 */
	public Address getAddress() {
		Account account = getAccount(0);
		Chain receiveChain = account.getChain(0);
		Address receiveAddress = receiveChain.getAddressAt(0);
		return receiveAddress;
	}

	/**
	 * Return BIP44 path for this wallet (m / purpose').
	 *
	 * @return String
	 *
	 */
	public String getPath() {
		return strPath;
	}

	/**
	 * Write entire wallet to JSONObject.
	 * For debugging only.
	 *
	 * @return JSONObject
	 * @throws Exception 
	 *
	 */
	public JSONObject toJSON() {
		try {
			JSONObject obj = new JSONObject();

			// add version info
			obj.put(JSON_VERSION, JSON_VERSION_VALUE);
			
			// add seed info
			if(seed != null) {
				if(strPassphrase != null && strPassphrase.length() > 0) {
					try {
						AesUtility aes = new AesUtility(strPassphrase);
						obj.put(JSON_SEED, aes.encrypt(AesUtility.bytesToBase64(seed)));
						obj.put(JSON_IV, aes.getIv());
					}
					catch (Exception e) {
						new RuntimeException(e.getMessage());
					}
				}
				else {
					obj.put(JSON_SEED, AesUtility.bytesToBase64(seed));
					obj.put(JSON_IV, "");
				}
			}

			// add account info
			JSONArray accts = new JSONArray();
			for(Account acct : accounts) {
				accts.put(acct.toJSON());
			}

			obj.put(JSON_ACCOUNTS, accts);

			return obj;
		}
		catch(JSONException ex) {
			throw new RuntimeException(ex);
		}
	}
}
