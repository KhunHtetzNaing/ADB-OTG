//
// Created by Rohit Verma on 15-01-2024.
//

#include "utils.h"

#include <string>
#include <fcntl.h>
#include <errno.h>
#include <unistd.h>

namespace adb {
    namespace file {
        static bool CleanUpAfterFailedWrite(const std::string &path) {
            int saved_errno = errno;
            unlink(path.c_str());
            errno = saved_errno;
            return false;
        }

        bool WriteStringToFd(std::string_view content, int fd) {
            const char *p = content.data();
            size_t left = content.size();
            while (left > 0) {
                ssize_t n = TEMP_FAILURE_RETRY(write(fd, p, left));
                if (n == -1) {
                    return false;
                }
                p += n;
                left -= n;
            }
            return true;
        }

        bool WriteStringToFile(const std::string &content, const std::string &path,
                               bool follow_symlinks) {
            int flags = O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC | O_BINARY |
                        (follow_symlinks ? 0 : O_NOFOLLOW);
            int fd = TEMP_FAILURE_RETRY(open(path.c_str(), flags, 0666));
            if (fd == -1) {
                return false;
            }
            bool ret = WriteStringToFd(content, fd) || CleanUpAfterFailedWrite(path);
            close(fd);
            return ret;
        }
    } // namespace file
} // namespace adb