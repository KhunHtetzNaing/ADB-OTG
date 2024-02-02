//
// Created by Rohit Verma on 15-01-2024.
//

#ifndef ADB_AUTH_H
#define ADB_AUTH_H

#include <string>
#include <stddef.h>

#define TOKEN_SIZE 20

constexpr size_t MAX_PAYLOAD = 1024 * 1024;

namespace adb {
    namespace auth {
        bool GenerateKey(const std::string &file);

        std::string GetPublicKey(std::string &file);

        std::string
        Sign(std::string &file, size_t max_payload, const char *token, size_t token_size);
    } // namespace auth
} // namespace adb

#endif // ADB_AUTH_H
