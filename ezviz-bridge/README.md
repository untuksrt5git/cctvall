# EZVIZ Bridge Test (Android)

A standalone test APK for the official EZVIZ Open Platform Android SDK. It is isolated from the main CCTV All-in-One app and does not change the existing dashboard, CloudCam, or MediaMTX setup.

## Current test scope

1. Initialize the official EZVIZ Global SDK.
2. Open the SDK's official EZVIZ account login page.
3. Query both `getDeviceList` and `getSharedDeviceList`.
4. Display device names, serials, and channels.
5. Attempt SDK live view using the official `EZPlayer`.

This first test does **not** export SDK video to RTSP or MediaMTX. That requires a separate supported stream/frame-output path and will be investigated only after account login, shared-device listing, and live view are confirmed.

## Developer setup required

- Create an application in the EZVIZ Open Platform developer console.
- Register the Android package name exactly as `id.cctvall.ezvizbridge`.
- Use the EZVIZ Open Platform **AppKey**. This is not the Uniview EZCloud AppID/SecretKey and not a camera password.
- The default region is Asia/Singapore, intended for Indonesia and other Asian countries outside China and Russia. If the EZVIZ account belongs to another region, select the matching region.
- The SDK's own login page handles account authentication. Do not enter the EZVIZ account password into a custom form or send credentials in chat.

The SDK dependency is pinned to `io.github.ezviz-open:ezviz-sdk:5.27.3`, matching the official Ezviz-Open Android sample project. The build includes ARMv7 and ARM64.

## Build

GitHub Actions workflow: `.github/workflows/build-ezviz-bridge.yml`.

Download the artifact named `EZVIZ-Bridge-Test-debug` after the workflow succeeds. Install the APK on the Android phone, enter the EZVIZ Open Platform AppKey, keep Asia/Singapore selected for an Indonesian account, and tap **Login resmi EZVIZ**.

## Important limitations

- Seeing devices in the SDK list is a separate test from playing them.
- Shared-device discovery is implemented using the SDK's `getSharedDeviceList` method, which is also used by EZVIZ's official Android sample.
- If live view requests a device verification code or returns an error, note the error code; do not share the verification code publicly.
- No claim is made yet that the SDK can expose a reusable RTSP URL or relay its video to MediaMTX.
