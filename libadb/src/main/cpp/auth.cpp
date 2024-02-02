//
// Created by Rohit Verma on 15-01-2024.
//

#include "auth.h"

#include <sys/stat.h>
#include <string>

#include <openssl/evp.h>
#include <openssl/bn.h>
#include <openssl/rsa.h>
#include <openssl/pem.h>

#include "logging.h"
#include "utils.h"
#include "crypto_utils.h"

namespace adb {
    namespace auth {
        using file::WriteStringToFile;

        static std::shared_ptr<RSA> ReadKeyFile(const std::string &file) {
            std::unique_ptr<FILE, decltype(&fclose)> fp(fopen(file.c_str(), "r"), fclose);
            if (!fp) {
                PLOGE("Failed to open '%s'", file.c_str());
                return nullptr;
            }

            RSA *key = RSA_new();
            if (!PEM_read_RSAPrivateKey(fp.get(), &key, nullptr, nullptr)) {
                LOGE("Failed to read key from '%s'", file.c_str());
                ERR_print_errors_fp(stderr);
                RSA_free(key);
                return nullptr;
            }
            return std::shared_ptr<RSA>(key, RSA_free);
        }

        static bool CalculatePublicKey(std::string *out, RSA *private_key) {
            uint8_t binary_key_data[PUBKEY_ENCODED_SIZE];
            if (!pubkey_encode(private_key, binary_key_data, sizeof(binary_key_data))) {
                LOGE("Failed to convert to public key");
                return false;
            }

            size_t expected_length;
            if (!EVP_EncodedLength(&expected_length, sizeof(binary_key_data))) {
                LOGE("Public key too large to base64 encode");
                return false;
            }

            out->resize(expected_length);
            size_t actual_length = EVP_EncodeBlock(reinterpret_cast<uint8_t *>(out->data()),
                                                   binary_key_data,
                                                   sizeof(binary_key_data));
            out->resize(actual_length);
            out->append(" ");
            out->append("unknown@unknown");
            return true;
        }

        bool GenerateKey(const std::string &file) {
            bssl::UniquePtr<EVP_PKEY> private_key(EVP_PKEY_new());
            bssl::UniquePtr<BIGNUM> exponent(BN_new());
            bssl::UniquePtr<RSA> rsa(RSA_new());
            if (!private_key || !exponent || !rsa) {
                LOGE("Failed to allocate key");
                return false;
            }

            BN_set_word(exponent.get(), RSA_F4);
            RSA_generate_key_ex(rsa.get(), 2048, exponent.get(), nullptr);
            EVP_PKEY_set1_RSA(private_key.get(), rsa.get());

            std::string pubkey;
            if (!CalculatePublicKey(&pubkey, rsa.get())) {
                LOGE("failed to calculate public key");
                return false;
            }

            mode_t old_mask = umask(077);

            std::unique_ptr<FILE, decltype(&fclose)> f(nullptr, &fclose);
            f.reset(fopen(file.c_str(), "w"));
            if (!f) {
                LOGE("Failed to open %s", file.c_str());
                umask(old_mask);
                return false;
            }

            umask(old_mask);

            if (!PEM_write_PrivateKey(f.get(), private_key.get(), nullptr, nullptr, 0, nullptr,
                                      nullptr)) {
                LOGE("Failed to write key");
                return false;
            }

            if (!WriteStringToFile(pubkey, file + ".pub")) {
                PLOGE("failed to write public key");
                return false;
            }
            return true;
        }

        std::string GetPublicKey(std::string &file) {
            std::shared_ptr<RSA> private_key = ReadKeyFile(file);
            if (!private_key) {
                return {};
            }

            std::string result;
            if (!CalculatePublicKey(&result, private_key.get())) {
                return {};
            }
            return result;
        }

        std::string
        Sign(std::string &file, size_t max_payload, const char *token, size_t token_size) {
            std::shared_ptr<RSA> private_key = ReadKeyFile(file);
            if (!private_key) {
                return {};
            }

            if (token_size != TOKEN_SIZE) {
                LOGD("Unexpected token size %zd", token_size);
                return {};
            }

            std::string result;
            result.resize(max_payload);

            unsigned int len;
            if (!RSA_sign(NID_sha1, reinterpret_cast<const uint8_t *>(token), token_size,
                          reinterpret_cast<uint8_t *>(&result[0]), &len, private_key.get())) {
                return {};
            }

            result.resize(len);

            LOGD("sign token len=%d", len);
            return result;
        }
    } // namespace auth
} // namespace adb