# Paper Wallet Generator for Bitcoin

## Application Description

Command line tool to create (offline) Bitcoin paper wallets.

## Demo Output

The output of the tool is a HTML page that can be viewed in any browser. 
An example output is provided below.
![HTML Page](/screenshots/paper_wallet_html.png)

As we want to create paper wallets, the CSS is prepared make the HTML printable.

![Printed Wallet](/screenshots/paper_wallet_printed.png)

## Run the Application

After cloning this repo build the command line tool using Maven.

```
mvn clean package
```

The result of the Maven build is an executable JAR file.

### Creating a Paper Wallet
 
Use the following command to create a paper wallet.

```
java -jar target/bpwg-0.1.0-SNAPSHOT.jar -d C:\Users\mzi\AppData\Local\Temp -p 'test pass phrase'
```

This will lead to some information on the console

```
creating wallet ...
wallet file successfully created
wallet pass phrase: 'test pass phrase'
wallet file location: C:\Users\mzi\AppData\Local\Temp\1NdarskdmUTo3eB4BiM2mSktBYQMdULXj4.json
writing additional output files ...
html wallet: C:\Users\mzi\AppData\Local\Temp\1NdarskdmUTo3eB4BiM2mSktBYQMdULXj4.html
address qr code: C:\Users\mzi\AppData\Local\Temp\1NdarskdmUTo3eB4BiM2mSktBYQMdULXj4.png
```

Three file are created by the tool as indicated in the output above
* The actual wallet file (1NdarskdmUTo3eB4BiM2mSktBYQMdULXj4.json)
* The HTML file for printing (1NdarskdmUTo3eB4BiM2mSktBYQMdULXj4.html)
* The image file with the QR code for the paper wallet address (1NdarskdmUTo3eB4BiM2mSktBYQMdULXj4.png)

### Verifying a (Paper) Wallet

The tool also allows to verify a provided wallet file against a provided pass phrase.

```
java -jar target/bpwg-0.1.0-SNAPSHOT.jar -p 'test pass phrase' -v C:\Users\mzi\AppData\Local\Temp\1NdarskdmUTo3eB4BiM2mSktBYQMdULXj4.json
```

This will lead to some information on the console

```
verifying wallet file ...
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
encrypted seed from wallet file: Wn9JknQMXgxGNdlniSn+9tulqCvZOuy1ed64Tj6bN84=
address successfully verified: M/44H/0H/0H/0/0 1NdarskdmUTo3eB4BiM2mSktBYQMdULXj4
address successfully verified: M/44H/0H/0H/0/1 14mBUkRtXn1SnXzktSn2KpTspAzk1yAN87
address successfully verified: M/44H/0H/0H/1/0 114RBko3EUPrAuTJmtwv7c1gopUTLEhNUV
address successfully verified: M/44H/0H/0H/1/1 1E256k68NyqBn7eFm4XgMtbaQieVX1a5oQ
mnemonics: document dove snap birth acoustic key jar civil book spawn cake girl
address: 1NdarskdmUTo3eB4BiM2mSktBYQMdULXj4
wallet file successfully verified
wallet file: C:\Users\mzi\AppData\Local\Temp\1NdarskdmUTo3eB4BiM2mSktBYQMdULXj4.json
pass phrase: test pass phrase
```

## Dependencies

The project [bitcoinj-bip44-extension](https://github.com/jonasbits/bitcoinj-bip44-extension) has served as an example for wallet file handling with the bitcoinj library. 

The project is maintained with the Eclipse IDE using Java 8. Building the project is done with Maven. 
For Bitcoin the [bitcoinj](https://bitcoinj.github.io/) library is used, 
to create QR codes the [ZXing](https://github.com/zxing/zxing) library and
for command line parsing the [JCommander](https://github.com/cbeust/jcommander) library.

