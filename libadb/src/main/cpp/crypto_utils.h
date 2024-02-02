//
// Created by Rohit Verma on 15-01-2024.
//

#ifndef ADB_CRYPTO_UTILS_H
#define ADB_CRYPTO_UTILS_H

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#include <openssl/rsa.h>

#ifdef __cplusplus
extern "C" {
#endif

#define PUBKEY_MODULUS_SIZE (2048 / 8)

#define PUBKEY_ENCODED_SIZE \
  (3 * sizeof(uint32_t) + 2 * PUBKEY_MODULUS_SIZE)

bool pubkey_decode(const uint8_t *key_buffer, size_t size, RSA **key);

bool pubkey_encode(const RSA *key, uint8_t *key_buffer, size_t size);

#ifdef __cplusplus
} // extern "C"
#endif

#endif // ADB_CRYPTO_UTILS_H
