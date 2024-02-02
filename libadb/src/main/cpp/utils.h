//
// Created by Rohit Verma on 15-01-2024.
//

#ifndef ADB_UTILS_H
#define ADB_UTILS_H

#include <string>
#include <unistd.h>

#if !defined(O_BINARY)
#define O_BINARY 0
#endif

#undef TEMP_FAILURE_RETRY
#ifndef TEMP_FAILURE_RETRY
#define TEMP_FAILURE_RETRY(exp)            \
  ({                                       \
    decltype(exp) _rc;                     \
    do {                                   \
      _rc = (exp);                         \
    } while (_rc == -1 && errno == EINTR); \
    _rc;                                   \
  })
#endif

namespace adb {
    namespace file {
        bool WriteStringToFd(std::string_view content, int fd);

        bool WriteStringToFile(const std::string &content, const std::string &path,
                               bool follow_symlinks = false);
    } // namespace file
} // namespace adb

#endif // ADB_UTILS_H
