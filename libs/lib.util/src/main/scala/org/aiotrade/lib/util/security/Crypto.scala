package org.aiotrade.lib.util.security

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey

object Crypto {

  def keyToByteArray(key: SecretKey): Array[Byte] = {
    val os = new ByteArrayOutputStream
    val out = new ObjectOutputStream(os)
    out.writeObject(key)
    out.close
    os.toByteArray
  }

  def keyFromByteArray(bytes: Array[Byte]): SecretKey = {
    val in = new ObjectInputStream(new ByteArrayInputStream(bytes))
    in.readObject.asInstanceOf[SecretKey]
  }

  def encrypt(content: String, key: SecretKey): Array[Byte] = {
    encrypt(content.getBytes("UTF-8"), key: SecretKey)
  }

  def encrypt(bytes: Array[Byte], key: SecretKey): Array[Byte] = {
    val mode = Cipher.ENCRYPT_MODE
    val cipher = Cipher.getInstance("AES")
    cipher.init(mode, key)
    crypt(bytes, cipher)
  }

  def decrypt(bytes: Array[Byte], key: SecretKey): Array[Byte] = {
    val mode = Cipher.ENCRYPT_MODE
    val cipher = Cipher.getInstance("AES")
    cipher.init(mode, key)
    crypt(bytes, cipher)
  }

  private def crypt(bytes: Array[Byte], cipher: Cipher): Array[Byte] = {
    val is = new ByteArrayInputStream(bytes)
    val result = crypt(is, cipher)
    is.close
    result
  }

  private def crypt(is: InputStream, cipher: Cipher): Array[Byte] = {
    val blockSize = cipher.getBlockSize
    val outputSize = cipher.getOutputSize(blockSize)
    val inBytes = new Array[Byte](blockSize)
    var outBytes = new Array[Byte](outputSize)

    val os = new ByteArrayOutputStream
    var inLength = 0
    var more = true
    while (more) {
      val inLength = is.read(inBytes)
      if (inLength == blockSize) {
        val outLength = cipher.update(inBytes, 0, blockSize, outBytes)
        os.write(outBytes, 0, outLength)
      } else {
        more = false
      }
    }

    if (inLength > 0) {
      outBytes = cipher.doFinal(inBytes, 0, inLength)
    } else {
      outBytes = cipher.doFinal
    }
    os.write(outBytes)

    os.close
    os.toByteArray
  }
}
