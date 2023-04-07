//
//  Created by dongSen on 2023/4/6.
//

#ifndef Mutex_h
#define Mutex_h

#include <mutex>
#include "config.h"

// Based On
// https://clang.llvm.org/docs/ThreadSafetyAnalysis.html#mutexheader

// Enable thread safety attributes only with clang.
// The attributes can be safely erased when compiling with other compilers.
#if defined(__clang__) && (!defined(SWIG))
#define CZXING_THREAD_ANNOTATION_ATTRIBUTE__(x)   __attribute__((x))
#else
#define CZXING_THREAD_ANNOTATION_ATTRIBUTE__(x)   // no-op
#endif

#define CZXING_CAPABILITY(x) CZXING_THREAD_ANNOTATION_ATTRIBUTE__(capability(x))

#define CZXING_SCOPED_CAPABILITY CZXING_THREAD_ANNOTATION_ATTRIBUTE__(scoped_lockable)

#define CZXING_GUARDED_BY(x) CZXING_THREAD_ANNOTATION_ATTRIBUTE__(guarded_by(x))

#define CZXING_PT_GUARDED_BY(x) CZXING_THREAD_ANNOTATION_ATTRIBUTE__(pt_guarded_by(x))

#define CZXING_ACQUIRED_BEFORE(...) \
  CZXING_THREAD_ANNOTATION_ATTRIBUTE__(acquired_before(__VA_ARGS__))

#define CZXING_ACQUIRED_AFTER(...) \
  CZXING_THREAD_ANNOTATION_ATTRIBUTE__(acquired_after(__VA_ARGS__))

#define CZXING_REQUIRES(...) \
  CZXING_THREAD_ANNOTATION_ATTRIBUTE__(requires_capability(__VA_ARGS__))

#define CZXING_REQUIRES_SHARED(...) \
  CZXING_THREAD_ANNOTATION_ATTRIBUTE__(requires_shared_capability(__VA_ARGS__))

#define CZXING_ACQUIRE(...) \
  CZXING_THREAD_ANNOTATION_ATTRIBUTE__(acquire_capability(__VA_ARGS__))

#define CZXING_ACQUIRE_SHARED(...) \
  CZXING_THREAD_ANNOTATION_ATTRIBUTE__(acquire_shared_capability(__VA_ARGS__))

#define CZXING_RELEASE(...) \
  CZXING_THREAD_ANNOTATION_ATTRIBUTE__(release_capability(__VA_ARGS__))

#define CZXING_RELEASE_SHARED(...) \
  CZXING_THREAD_ANNOTATION_ATTRIBUTE__(release_shared_capability(__VA_ARGS__))

#define CZXING_TRY_ACQUIRE(...) \
  CZXING_THREAD_ANNOTATION_ATTRIBUTE__(try_acquire_capability(__VA_ARGS__))

#define CZXING_TRY_ACQUIRE_SHARED(...) \
  CZXING_THREAD_ANNOTATION_ATTRIBUTE__(try_acquire_shared_capability(__VA_ARGS__))

#define CZXING_EXCLUDES(...) CZXING_THREAD_ANNOTATION_ATTRIBUTE__(locks_excluded(__VA_ARGS__))

#define CZXING_ASSERT_CAPABILITY(x) CZXING_THREAD_ANNOTATION_ATTRIBUTE__(assert_capability(x))

#define CZXING_ASSERT_SHARED_CAPABILITY(x) \
  CZXING_THREAD_ANNOTATION_ATTRIBUTE__(assert_shared_capability(x))

#define CZXING_RETURN_CAPABILITY(x) CZXING_THREAD_ANNOTATION_ATTRIBUTE__(lock_returned(x))

#define CZXING_NO_THREAD_SAFETY_ANALYSIS \
  CZXING_THREAD_ANNOTATION_ATTRIBUTE__(no_thread_safety_analysis)

CZXING_BEGIN_NAMESPACE();

// NOTE: Wrappers for std::mutex and std::unique_lock are provided so that
// we can annotate them with thread safety attributes and use the
// -Wthread-safety warning with clang. The standard library types cannot be
// used directly because they do not provided the required annotations.
class CZXING_CAPABILITY("mutex") Mutex {
 public:
  Mutex() {}

  void lock() CZXING_ACQUIRE() { mut_.lock(); }
  void unlock() CZXING_RELEASE() { mut_.unlock(); }
  std::mutex& native_handle() { return mut_; }

 private:
  std::mutex mut_;
};

class CZXING_SCOPED_CAPABILITY UniqueLock {
  typedef std::unique_lock<std::mutex> UniqueLockImp;

 public:
  UniqueLock(Mutex& m) CZXING_ACQUIRE(m) : ul_(m.native_handle()) {}
  ~UniqueLock() CZXING_RELEASE() {}
  UniqueLockImp& native_handle() { return ul_; }

 private:
  UniqueLockImp ul_;
};

class CZXING_SCOPED_CAPABILITY LockGuard {
  typedef std::lock_guard< std::mutex > LockGuardImp;
public:
  LockGuard(Mutex& m) CZXING_ACQUIRE(m) : lg_(m.native_handle()) {}
  ~LockGuard() CZXING_RELEASE() {}
  LockGuardImp& native_handle() { return lg_; }
private:
  LockGuardImp lg_;
};

class CZXING_SCOPED_CAPABILITY MutexLocker {
public:
  // Acquire mu, implicitly acquire *this and associate it with mu.
  MutexLocker(Mutex& mu) CZXING_ACQUIRE(mu) : mut(mu), locked(true) { mu.lock(); }
  ~MutexLocker() CZXING_RELEASE() { if (locked) mut.unlock(); }

  void lock() CZXING_ACQUIRE() { mut.lock(); locked = true; }
  void unlock() CZXING_RELEASE() { locked = false; mut.unlock(); }
    
private:
  Mutex& mut;
  bool locked;
};

CZXING_END_NAMESPACE();

#endif /* Mutex_h */
