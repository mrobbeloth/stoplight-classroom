// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "StoplightClassroom",
    platforms: [.macOS(.v14)],
    dependencies: [],
    targets: [
        .executableTarget(
            name: "StoplightClassroom",
            path: "Sources"
        )
    ]
)
