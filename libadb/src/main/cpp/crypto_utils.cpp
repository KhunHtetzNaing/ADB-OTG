//
// Created by Rohit Verma on 15-01-2024.
//

#include "crypto_utils.h"

#include <assert.h>
#include <stdlib.h>
#include <string.h>

#include <openssl/bn.h>

#define PUBKEY_MODULUS_SIZE_WORDS (PUBKEY_MODULUS_SIZE / 4)

struct RSAPublicKey {
    uint32_t modulus_size_words;
    uint32_t n0inv;
    uint8_t modulus[PUBKEY_MODULUS_SIZE];
    uint8_t rr[PUBKEY_MODULUS_SIZE];
    uint32_t exponent;
};

bool pubkey_decode(const uint8_t *key_buffer, size_t size, RSA **key) {
    const RSAPublicKey *key_struct = (RSAPublicKey *) key_buffer;

    bool ret = false;

    RSA *new_key = RSA_new();
    BIGNUM *n = NULL;
    BIGNUM *e = NULL;
    if (!new_key) {
        goto cleanup;
    }

    if (size < sizeof(RSAPublicKey)) {
        goto cleanup;
    }

    if (key_struct->modulus_size_words != PUBKEY_MODULUS_SIZE_WORDS) {
        goto cleanup;
    }

    n = BN_le2bn(key_struct->modulus, PUBKEY_MODULUS_SIZE, NULL);
    if (!n) {
        goto cleanup;
    }

    e = BN_new();
    if (!e || !BN_set_word(e, key_struct->exponent)) {
        goto cleanup;
    }

    if (!RSA_set0_key(new_key, n, e, NULL)) {
        goto cleanup;
    }

    n = NULL;
    e = NULL;

    *key = new_key;
    new_key = NULL;
    ret = true;

    cleanup:
    RSA_free(new_key);
    BN_free(n);
    BN_free(e);
    return ret;
}

bool pubkey_encode(const RSA *key, uint8_t *key_buffer, size_t size) {
    RSAPublicKey *key_struct = (RSAPublicKey *) key_buffer;

    bool ret = false;

    BN_CTX *ctx = BN_CTX_new();
    BIGNUM *r32 = BN_new();
    BIGNUM *n0inv = BN_new();
    BIGNUM *rr = BN_new();

    if (sizeof(RSAPublicKey) > size || RSA_size(key) != PUBKEY_MODULUS_SIZE) {
        goto cleanup;
    }

    key_struct->modulus_size_words = PUBKEY_MODULUS_SIZE_WORDS;

    if (!ctx || !r32 || !n0inv || !BN_set_bit(r32, 32) ||
        !BN_mod(n0inv, RSA_get0_n(key), r32, ctx) ||
        !BN_mod_inverse(n0inv, n0inv, r32, ctx) || !BN_sub(n0inv, r32, n0inv)) {
        goto cleanup;
    }

    key_struct->n0inv = (uint32_t) BN_get_word(n0inv);

    if (!BN_bn2le_padded(key_struct->modulus, PUBKEY_MODULUS_SIZE, RSA_get0_n(key))) {
        goto cleanup;
    }

    if (!ctx || !rr || !BN_set_bit(rr, PUBKEY_MODULUS_SIZE * 8) ||
        !BN_mod_sqr(rr, rr, RSA_get0_n(key), ctx) ||
        !BN_bn2le_padded(key_struct->rr, PUBKEY_MODULUS_SIZE, rr)) {
        goto cleanup;
    }

    key_struct->exponent = (uint32_t) BN_get_word(RSA_get0_e(key));

    ret = true;

    cleanup:
    BN_free(rr);
    BN_free(n0inv);
    BN_free(r32);
    BN_CTX_free(ctx);
    return ret;
}
