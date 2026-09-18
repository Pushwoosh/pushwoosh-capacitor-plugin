// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "PushwooshCapacitorPlugin",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "PushwooshCapacitorPlugin",
            targets: ["PushwooshPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0"),
        // tools/release.bash rewrites this line's version via a sed anchored on its exact formatting — keep it on one line, spacing unchanged.
        // exact: is deliberate, mirroring the podspec's hard pin: relaxing to from: would let CocoaPods and SPM consumers land on different native SDK versions.
        .package(url: "https://github.com/Pushwoosh/Pushwoosh-XCFramework.git", exact: "7.2.6")
    ],
    targets: [
        .target(
            name: "PushwooshPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm"),
                .product(name: "PushwooshFramework", package: "Pushwoosh-XCFramework"),
                .product(name: "PushwooshCore", package: "Pushwoosh-XCFramework"),
                .product(name: "PushwooshBridge", package: "Pushwoosh-XCFramework")
            ],
            path: "ios/Sources/PushwooshPlugin"),
        .testTarget(
            name: "PushwooshPluginTests",
            dependencies: ["PushwooshPlugin"],
            path: "ios/Tests/PushwooshPluginTests")
    ]
)