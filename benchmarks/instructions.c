#include <jni.h>
#include <jvmti.h>
#include <stdint.h>
#include <string.h>

static jvmtiEnv *ti;
static jlong count;
static int active;

static void check(JNIEnv *env, jvmtiError error) {
    if (error != JVMTI_ERROR_NONE) {
        (*env)->FatalError(env, "JVMTI instruction counter failed");
    }
}

static void JNICALL step(jvmtiEnv *jvmti, JNIEnv *env, jthread thread,
                         jmethodID method, jlocation location) {
    (void) jvmti; (void) env; (void) thread; (void) method; (void) location;
    count++;
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
    (void) options; (void) reserved;
    if ((*vm)->GetEnv(vm, (void **) &ti, JVMTI_VERSION_1_2) != JNI_OK) return JNI_ERR;
    jvmtiCapabilities capabilities;
    memset(&capabilities, 0, sizeof(capabilities));
    capabilities.can_generate_single_step_events = 1;
    if ((*ti)->AddCapabilities(ti, &capabilities) != JVMTI_ERROR_NONE) return JNI_ERR;
    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.SingleStep = step;
    if ((*ti)->SetEventCallbacks(ti, &callbacks, sizeof(callbacks)) != JVMTI_ERROR_NONE) return JNI_ERR;
    return JNI_OK;
}

/* The harness is deliberately single-threaded. Events are enabled only on its thread. */
JNIEXPORT void JNICALL Java_InstructionBenchmark_start(JNIEnv *env, jclass cls) {
    (void) cls;
    if (!ti || active) { (*env)->FatalError(env, "Invalid instruction counter start"); return; }
    jthread thread;
    check(env, (*ti)->GetCurrentThread(ti, &thread));
    count = 0;
    active = 1;
    check(env, (*ti)->SetEventNotificationMode(ti, JVMTI_ENABLE, JVMTI_EVENT_SINGLE_STEP, thread));
    (*env)->DeleteLocalRef(env, thread);
}

JNIEXPORT jlong JNICALL Java_InstructionBenchmark_stop(JNIEnv *env, jclass cls) {
    (void) cls;
    if (!active) { (*env)->FatalError(env, "Invalid instruction counter stop"); return 0; }
    jthread thread;
    check(env, (*ti)->GetCurrentThread(ti, &thread));
    check(env, (*ti)->SetEventNotificationMode(ti, JVMTI_DISABLE, JVMTI_EVENT_SINGLE_STEP, thread));
    (*env)->DeleteLocalRef(env, thread);
    active = 0;
    return count;
}
