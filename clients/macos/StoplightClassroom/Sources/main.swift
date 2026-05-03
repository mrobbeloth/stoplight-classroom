import Foundation

/// API client for Stoplight Classroom REST API
class StoplightAPI {
    let baseURL: String
    var token: String?

    init(baseURL: String) {
        self.baseURL = baseURL
    }

    func login(email: String, password: String) async throws -> AuthResponse {
        let body = ["email": email, "password": password]
        return try await post("/api/student/auth/login", body: body)
    }

    func joinSession(joinCode: String, displayName: String) async throws -> JoinResponse {
        let body = ["joinCode": joinCode, "displayName": displayName]
        return try await post("/api/sessions/join", body: body)
    }

    func submitStoplight(sessionId: Int, value: String) async throws {
        let body = ["value": value]
        let _: EmptyResponse = try await post("/api/stoplight/\(sessionId)", body: body)
    }

    private func post<T: Decodable>(_ path: String, body: [String: String]) async throws -> T {
        var request = URLRequest(url: URL(string: baseURL + path)!)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let token = token {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(T.self, from: data)
    }
}

struct AuthResponse: Decodable {
    let accessToken: String
    let refreshToken: String
}

struct JoinResponse: Decodable {
    let participantId: Int
    let sessionId: Int
    let participantToken: String
    let activityMode: String
}

struct EmptyResponse: Decodable {}

// Entry point — placeholder for SwiftUI app
print("Stoplight Classroom macOS Client")
print("Configure baseURL to your server and build with SwiftUI.")
