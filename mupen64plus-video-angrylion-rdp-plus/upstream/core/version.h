#pragma once

#define GIT_BRANCH "master"
#define GIT_TAG "r4-65"
#define GIT_COMMIT_HASH "d5e8ac5324d5bcece5dccd377b728ae520af7a64"
#define GIT_COMMIT_DATE "2017-10-10 00:16:17 +0200"

#define CORE_BASE_NAME "angrylion's RDP Plus"

#ifdef _DEBUG
#define CORE_NAME CORE_BASE_NAME " " GIT_TAG " (Debug)"
#else
#define CORE_NAME CORE_BASE_NAME " " GIT_TAG
#endif

#define CORE_SIMPLE_NAME "angrylion-plus"
