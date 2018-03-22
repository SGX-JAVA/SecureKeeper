# Part 1: Confidentiality

**Threat:** An attacker who controls the communication network. This attacker can arbitrarily read, modify, and delete messages. Think of communication model as one in which messages are always sent to the attacker, never to the intended recipient. The attacker can then forward the message along if he chooses, redirect the message, save it for later replay, etc. This kind of threat is called a *Dolev-Yao* attacker.

**Harm:**  Messages containing secret information could be disclosed to the adversary, thus violating confidentiality.

**Vulnerability:** The communication channel between sender and receiver can be read by untrusted principals.

**Countermeasure:** Encryption.

```
Shared-key Encryption

   1. Alice: c = Enc(m; k)
   2. Alice -> Bob: c
   3. Bob: m = Dec(c; k)
```
(The format we use above is a *protocol narration*: each step is numbered and is either a computation or a message. We identify principal(s) involved at each step by writing their names followed by a colon,)

Enc is the encryption algorithm; Dec is decryption. Alice and Bob must somehow *share* a key k that has previously been generated:

0. k = Gen(len) // len is length of key
1. ...

Together, (Gen, Enc, Dec) constitute an *encryption* scheme or *cryptosystem*. Well known examples of encryption schemes include AES (which uses shared key) and RSA (which does not).

### Block Ciphers
Efficient encryption schemes usually operate on fixed-size messages called *blocks*. Such schemes are called *block ciphers*.

### Block Cipher Modes
If block ciphers work only on fixed length blocks, how can we send longer messages than block length? A *block cipher mode* is an algorithm that uses a fixed-length block cipher to send an arbitrary-length message.

**Strawman idea:** chunk message into blocks; encrypt each block individually.  Ciphertext block number i, written c_i, is thus Enc(m_i; k) , where m_i is plaintext block number i. This algorithm is called *electronic codebook mode* (ECB).

```
ECB:
c_i = Enc(m_i; k)
```
ECB is a **BAD IDEA** that unfortunately gets invented over and over again, especially by students of crypto. Why is it bad? Because any two blocks that are same in plaintext will be same in ciphertext. (Wikipedia has a nice graphical illustration of how ECB fails to provide confidentiality.) **Do not use ECB.** Unfortunately, it is still the default in Java, but you don't have to settle for the default.

One of best-known, good block cipher modes is *cipher block chaining* (CBC). With it, every ciphertext block depends on **all** previous ciphertext blocks, which avoids repetition problems like we observed with ECB.

```
CBC:
c_i = Enc(m_i XOR c_{i-1}; k)
```
If the first plaintext block is m_1, what is c_0? It can't be the encryption of a plaintext block; it has to be somehow invented from scratch for each new encryption. Block c_0 is, therefore, called the *initialization vector* (IV). It must be unpredictable to attackers for CBC to be secure. The best practice is to choose a new IV randomly for each (multi-block) message. The IV is sent in the clear, without encryption, because there is no meaningful information in it.

Another good block cipher mode is *counter mode* (CTR):
```
CTR:
k_i = Enc(n, i; k)
c_i = m_i XOR k_i
```
CTR uses Enc to encrypt a *nonce* n and a counter i in the same plaintext. Observe the notation we use for that: commas between the parts of the plaintext that we are combining into one message, and a semi-colon before the key. In an implementation, we could use bit concatenation to combine message parts. Like the IV in CBC, nonce n should be randomly chosen for each new message (but stays the same for each block in the stream for a given message), and can be sent in the clear as ciphertext block c0.

An advantage of CTR over CBC is that each block in CTR can be computed in parallel, whereas CBC must process the blocks sequentially.

### Nonces
Both n and the IV in the modes above are examples of a *nonce*: a number used once. Nonces show up a lot in crypto. A nonce must always be

- **Unique**, meaning that it has never been used before in the lifetime of the system (A synonym for "unique" is "fresh".) Nonces may also be
- **Unpredictable**, meaning that it isn't possible to predict the next nonce, even given knowledge of all the nonces that have been used for far in the lifetime of a system.

### Public-Key Cryptography
There's a big problem with the encryption schemes we've examined so far; the shared keys have to be distributed. For each pair of principals who want to communicate, a key needs to be shared. If there are n principals, that's O(n^2) keys. That sharing costs time and money.

This problem motivated the invention of another kind of encryption scheme: *asymmetric* or *public key cryptography* . RSA is the most famous example. The name "asymmetric" comes from the fact that different keys are used for encryption vs. decryption. In *symmetric* schemes like AES, the same key is used for both encryption and decryption.

In a public-key cryptosystem, every principal has its own *key pair*, comprising a
- **public key,** which is published for the world to see, and a
- **private key,** which is kept secret and never shared with anyone.

With public-key schemes, key distribution becomes much easier. We need only to publish a "phonebook" of public keys, which contains just O(n) keys. Thus we reduce from a quadratic problem to a linear problem.

```
Public-key Encryption:

   0. Bob: (K_B, k_B) = Gen(len)
   1. Alice: c = Enc(m; K_B)
   2. Alice → Bob: c
   3. Bob: m = Dec(c; k_B)
```
Note how we use upper-case K for public keys and lower-case k for private keys.

# Part 2: Integrity
**Threat:** A Dolev-Yao attacker.

**Harm:** The information contained in messages could be modified, thus violating integrity.

**Harm:** The purported sender of a message could be changed, thus violating integrity.

**Vulnerability:** Messages sent on the communication channel between the sender and receiver can be modified by untrusted principals.

**Countermeasure:** MACs and digital signatures.

Like encryption, there are symmetric and asymmetric algorithms for protecting integrity. The symmetric version is called a *message authentication codes* (MAC). The asymmetric version is called a *digital signature*. Both use another primitive, hash functions, which we'll cover first.

### Cryptographic Hash Functions
A *cryptographic hash function*, also called a *message digest*, takes an arbitrary size input m and produces a length output H(m).

The goal of a cryptographic hash is to produce a compact representation of an original object. That representation should behave much like a fingerprint:

- It's hard to find 2 people with same fingerprint. That's true whether you get to pick pairs of people, or whether you are given one person then must find another. That means fingerprints are **collision resistant**.
- Given a person, it's easy to get their fingerprint. But given a fingerprint, it's hard to find the person it came from. (Which is why law enforcement invests money in building databases to do just that.) That means fingerprints are **one way**.

Cryptographic hash functions are not the same as the ordinary hash functions that are used to implement hash tables, even though both compress their inputs. Collision resistance and one way-ness are not required for ordinary hash functions.

MD5 and SHA-1 used to be the most commonly used hash functions.

### Message Authentication Codes (MACs)

A *message authentication code* is an algorithm for detecting modification of messages based on a shared key.

```
MAC:


  0. k = Gen(len) // A and B somehow share key k
  1. A: t = MAC(m; k) // t is called the "tag"
  2. A->B: m, t
  3. B: verify t = MAC(m; k)
```

There are many examples of MACs. HMAC(a hash-based MAC) is one of the most common.
```
HMAC(m; k) = H(f1(k), H(f2(k), m)) 
```
Function H is a cryptographic hash function.

Note that MACs do not protect confidentiality, at least necessarily. Some happen to do so, but it's easy to construct MACs that don't.

### Digital Signatures

A *digital signature scheme* is a set of algorithms for detecting modification of messages based on a public-private key pair. The public key for principal for A, written K_A, is used to verify A's signatures. The private key for principal A, written k_A, is used by A to create signatures.

```
Digital Signature:

    0. (K_A, k_A) = Gen(len)
    1. A: s = Sign(m; k_A)
    2. A->B: m, s
    3. B: accept if Ver(m; s; K_A)
```
The digital signature scheme is the triple (Gen, Sign, Ver) of algorithms. Note that Ver takes three inputs: the message to verify, the purported signature on that message, and the verification key of the signer.

As with MACs, we want to be able to sign arbitrary length messages. But these Sign and Ver are public-key algorithms, which operate on big integers. So, as with public-key encryption, they are constrained to a limited size. 

In practice, messages are therefore hashed before being signed:
```
Digital Signature with Hashing:

    0. (K_A, k_A) = Gen(len)
    1. A: s = Sign(H(m); k_A)
    2. A->B: m, s
    3. B: accept if Ver(H(m); s; K_A)
```

# Part 3: Confidentiality and Integrity
### Authenticated Encryption
Suppose you want to protect both confidentiality and integrity. The result is called *authenticated encryption*. There are three generic ways of constructing authenticated encryption out of a standard block cipher and MAC. All three are used in real-world protocols.

- **MAC then Encrypt.** MAC the plaintext message. Encrypt the message and the tag together. Send the resulting ciphertext. As long as the MAC algorithm is strong enough (and HMAC is), this algorithm is just as secure as the previous one. SSL uses this algorithm.

There are also block cipher modes that are specifically designed to achieve both confidentiality and integrity.

### Secure Sockets Layer (SSL)
Authenticated encryption is such a massively useful thing that it's long been available as part of libraries and other software distributions. Netscape introduced a protocol for it back in 1996 called Secure Sockets Layer (SSL) v3. SSL essentially provides authenticated encryption on top of TCP.

SSL is used widely—for example, HTTPS is just HTTP run over SSL. SSL was standardized under the name Transport Layer Security (TLS), so you'll see it referred to by either name in the literature.

TLS manages *sessions*, which are bi-directional communication between a *client* and a *server*. The communication is optionally secured for both confidentiality and integrity against a Dolev-Yao attacker. Sessions are *logical*: there can be many sessions between any two physical hosts, and each host could be either client or server in any given session.

Each message sent during a session is called a *record*. Records are protected by MAC-then-Encrypt. The MAC used is HMAC. The hash function and encryption scheme used can be negotiated by the client and server for each SSL session. Digital signatures and certificates can used to negotiate the shared encryption and MAC keys for each session. We'll look more at the details of this negotiation when we discuss authentication of machines.
