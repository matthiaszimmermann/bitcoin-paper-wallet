package org.matthiaszimmermann.bitcoin.pwg;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException.MnemonicChecksumException;
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException;
import org.bitcoinj.crypto.MnemonicException.MnemonicWordException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain.KeyPurpose;
import org.bitcoinj.wallet.KeyChainGroup;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.junit.Assert;
import org.junit.Test;

public class BitcoinjTest {

	@Test
	public void testSeeds() throws MnemonicLengthException, MnemonicWordException, MnemonicChecksumException {
		int bits = 128;
		byte [] entropy = getEntropy(bits);
		List<String> mnemonic = MnemonicCode.INSTANCE.toMnemonic(entropy);		
		String passphrase = "";
		long creationTimeSeconds = System.currentTimeMillis() / 1000;

		DeterministicSeed ds1 = new DeterministicSeed(entropy, passphrase, creationTimeSeconds);
		DeterministicSeed ds2 = new DeterministicSeed(mnemonic, null, passphrase, creationTimeSeconds);

		byte [] ds1seed = ds1.getSeedBytes();
		byte [] ds2seed = ds2.getSeedBytes();

		Assert.assertArrayEquals(ds1seed, ds2seed);

		List<String> ds1mnemonic = ds1.getMnemonicCode();
		List<String> ds2mnemonic = ds2.getMnemonicCode();

		assertEquals(mnemonic, ds1mnemonic);
		assertEquals(ds1mnemonic, ds2mnemonic);

		printMnemonic(mnemonic);
	}

	@Test
	public void testWallet() throws UnreadableWalletException {
		long creationTimeSeconds = System.currentTimeMillis() / 1000;
		NetworkParameters params = MainNetParams.get();
		String [] MNEMONIC = new String [] {"sword","acquire","little","despair","wave","swear","during","expect","target","science","banana","eyebrow"};
		String RECEIVE_ADDRESS = "13w74gmmSjb2DWit5iRQbCKPZj89FkCidH";
		String CHANGE_ADDRESS = "1hF37FtrcSyEFkyxo2Q3541aGfWRqsuLV";
		
		List<String> fixedMnemonic = toMnemonic(MNEMONIC);
		printMnemonic(fixedMnemonic);
		
		DeterministicSeed ds2 = new DeterministicSeed(fixedMnemonic, null, "", creationTimeSeconds);
		
		KeyChainGroup fixedKeyChainFromSeed = new KeyChainGroup(params, ds2);
		Wallet fixedWallet = new Wallet(params, fixedKeyChainFromSeed);
		
		String fixedReceiveAddress = fixedWallet.currentReceiveAddress().toString();
		String fixedChangeAddress = fixedWallet.currentChangeAddress().toString();

		Assert.assertEquals(RECEIVE_ADDRESS, fixedReceiveAddress);
		Assert.assertEquals(CHANGE_ADDRESS, fixedChangeAddress);
		
		printAddress(fixedKeyChainFromSeed.currentAddress(KeyPurpose.AUTHENTICATION), "current auth address");
		printAddress(fixedKeyChainFromSeed.currentAddress(KeyPurpose.CHANGE), "current change address");
		printAddress(fixedKeyChainFromSeed.currentAddress(KeyPurpose.RECEIVE_FUNDS), "current receive address");
		printAddress(fixedKeyChainFromSeed.currentAddress(KeyPurpose.REFUND), "current refund address");
		
		for(int i = 0; i < 10; i++) {
			System.out.println("address[" + i + "]: "+ fixedKeyChainFromSeed.freshAddress(KeyPurpose.RECEIVE_FUNDS).toString());
		}
		
		System.out.println("---------------");
		
		String MNEMONIC_STRING = "sword acquire little despair wave swear during expect target science banana eyebrow";
		DeterministicSeed seed = new DeterministicSeed(MNEMONIC_STRING, null, "", creationTimeSeconds);
		Wallet wallet = Wallet.fromSeed(params, seed);
		
		System.out.println("wallet receive address: " + wallet.currentReceiveAddress().toString());
	}
	
	@Test
	public void newWalletTest() throws AddressFormatException, MnemonicLengthException, MnemonicWordException, MnemonicChecksumException, IOException, DecoderException {
		String MNEMONIC_STRING = "sword acquire little despair wave swear during expect target science banana eyebrow";
		String passphrase = "";
		org.matthiaszimmermann.bitcoin.pwg.Wallet hdWallet =  WalletFactory.getInstance().restoreWallet(MNEMONIC_STRING, passphrase);
		System.out.println(hdWallet.toJSON().toString(4));
	}

	private void printMnemonic(List<String> mnemonic) {
		System.out.print(String.format("mnemonic (%s):", mnemonic.size()));
		mnemonic.forEach(word -> System.out.print(" " + word));
		System.out.println();
	}
	
	private List<String> toMnemonic(String [] words) {
		List<String> mnemonic = new ArrayList<>();
		
		for(int i = 0; i < words.length; i++) {
			mnemonic.add(words[i]);
		}
		
		return mnemonic;
	}
	
	private void printAddress(Address address, String info) {
		System.out.println(info + " " + address.toString());
	}

	private static byte[] getEntropy(int bits) {
		return getEntropy(new SecureRandom(), bits);
	}

	private static byte[] getEntropy(SecureRandom random, int bits) {
		checkArgument(bits <= DeterministicSeed.MAX_SEED_ENTROPY_BITS, "requested entropy size too large");

		byte[] seed = new byte[bits / 8];
		random.nextBytes(seed);
		return seed;
	}

	private static void assertEquals(List<String> a, List<String> b) {
		Assert.assertArrayEquals(a.toArray(new String[a.size()]), b.toArray(new String[b.size()]));
	}
}
