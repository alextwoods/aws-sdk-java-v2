$version: "2.0"

namespace test.weather

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restJson1

/// One AWS service, with the traits the client configuration derives its names from.
@service(sdkId: "Weather Forecast", endpointPrefix: "forecast", arnNamespace: "weather")
@sigv4(name: "weather-signing")
@restJson1
service WeatherService {
    version: "2024-01-01"
    operations: [GetForecast]
}

@http(method: "GET", uri: "/forecast/{city}")
@readonly
operation GetForecast {
    input := {
        @required
        @httpLabel
        city: String
    }
    output := {
        summary: String
    }
}
