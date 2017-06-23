package org.matthiaszimmermann.bitcoin.pwg;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException.MnemonicChecksumException;
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException;
import org.bitcoinj.crypto.MnemonicException.MnemonicWordException;
import org.bitcoinj.params.MainNetParams;
import org.json.JSONObject;
import org.junit.Test;

public class WalletTest extends BaseTest {
	
	public static final String WALLET_MNEMONIC = "angle end trade shiver title learn shove top wonder exotic lamp puppy";
	public static final String WALLET_SEED = "08c9339ae31e2efdb1c727fd0a01f357";
	public static final byte [] WALLET_SEED_BYTES = {8, -55, 51, -102, -29, 30, 46, -3, -79, -57, 39, -3, 10, 1, -13, 87};
	public static final String WALLET_PASS_PHRASE = "test pass phrase";
	public static final String WALLET_JSON_OK = "{\"seed\":\"dQYSjX6+aGmzn7AhDZhNJnIx8rvjfttgCQL0khMums4=\",\"accounts\":[{\"chains\":[{\"path\":\"M/44H/0H/0H/0\",\"addresses\":[{\"path\":\"M/44H/0H/0H/0/0\",\"address\":\"1FteBgh6KQ3Bnv4SSx8r2oLE198uExs5Te\"},{\"path\":\"M/44H/0H/0H/0/1\",\"address\":\"1JcGxWyYoqnU9DHGnt8RsKYyz738bKvydU\"}]},{\"path\":\"M/44H/0H/0H/1\",\"addresses\":[{\"path\":\"M/44H/0H/0H/1/0\",\"address\":\"1JcUxdTcE5UcCCFAv2QwoVPqWNFxo6VB57\"},{\"path\":\"M/44H/0H/0H/1/1\",\"address\":\"18gXGiQ2dfCVTFioJp35bTANjzmVczyyHA\"}]}]}],\"version\":\"1.0\",\"iv\":\"7RFrnKNxd+xIUlFYK05cMw==\"}"; 

	@Test
	public void testWalletFromJson() throws Exception {
		JSONObject node = new JSONObject(WALLET_JSON_OK);
		Wallet wallet = WalletFactory.getInstance().restoreWalletFromJSON(node, WALLET_PASS_PHRASE);
		
		assertEquals(WALLET_SEED, wallet.getSeedHex());
		assertArrayEquals(WALLET_SEED_BYTES, wallet.getSeed());
		assertEquals(WALLET_MNEMONIC, wallet.getMnemonic());
		assertEquals(WALLET_PASS_PHRASE, wallet.getPassphrase());
	}

	@Test
	public void testWalletFromSeed() throws Exception {
		NetworkParameters params = MainNetParams.get();
		MnemonicCode mc = new MnemonicCode();
		Wallet wallet = new Wallet(mc, params, WALLET_SEED_BYTES, WALLET_PASS_PHRASE);
		
		assertEquals(WALLET_SEED, wallet.getSeedHex());
		assertArrayEquals(WALLET_SEED_BYTES, wallet.getSeed());
		assertEquals(WALLET_MNEMONIC, wallet.getMnemonic());
		assertEquals(WALLET_PASS_PHRASE, wallet.getPassphrase());		
	}
	
	@Test
	public void testDummyFromMnemonics() throws IOException, MnemonicLengthException, MnemonicWordException, MnemonicChecksumException {
		String words = "sword acquire little despair wave swear during expect target science banana eyebrow";
		NetworkParameters params = MainNetParams.get();
		MnemonicCode mc = new MnemonicCode();
		words = words.replaceAll("[^a-z]+", " "); // assume BIP39 English
		List<String> mnemonic =  Arrays.asList(words.trim().split("\\s+"));
        byte [] seed = mc.toEntropy(mnemonic);
		
		Wallet wallet = new Wallet(mc, params, seed, WALLET_PASS_PHRASE);
		log("wallet in json format:\n%s", wallet.toJSON().toString(2));
	}
	
	@Test
	public void testWalletFromMnemonics() throws Exception {
		NetworkParameters params = MainNetParams.get();
		MnemonicCode mc = new MnemonicCode();
		List<String> mnemonic = toList(WALLET_MNEMONIC);
		Wallet wallet = new Wallet(mc, params, mnemonic, WALLET_PASS_PHRASE);
		
		assertEquals(WALLET_SEED, wallet.getSeedHex());
		assertArrayEquals(WALLET_SEED_BYTES, wallet.getSeed());
		assertEquals(WALLET_MNEMONIC, wallet.getMnemonic());
		assertEquals(WALLET_PASS_PHRASE, wallet.getPassphrase());		
	}

	private List<String> toList(String mnemonic) {
		List<String> list = new ArrayList<>();
		
		for(String word: mnemonic.split(" ")) {
			list.add(word);
		}
		
		return list;
	}
}
