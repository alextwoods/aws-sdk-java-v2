$version: "2"
namespace example.weather

@aws.protocols#awsJson1_0
@aws.auth#sigv4(name: "weather")
@custom#meta({ tier: 1 })
service Weather {
    version: "2006-03-01"
    operations: [GetForecast]
    errors: [Throttled]
}

@readonly
operation GetForecast {
    input := {
        @required
        @length(min: 1, max: 64)
        city: CityName
        days: Days = 3
    }
    output := {
        forecast: Forecast
        unit: TemperatureUnit
        level: Level
    }
    errors: [NoSuchCity]
}

@pattern("^[A-Za-z ]+$")
string CityName

@range(min: 1, max: 14)
integer Days

structure Forecast {
    summary: String
    @required
    days: DayList
    next: Forecast
}

list DayList {
    member: Day
}

union Day {
    sunny: Unit
    @custom#note("wet")
    rain: RainDetail
}

structure RainDetail {
    millimeters: Double
    @jsonName("next")
    forecast: Forecast
}

enum TemperatureUnit {
    @custom#note("default")
    @custom#other([1, 2.5, "x\u00e9"])
    CELSIUS = "C"
    FAHRENHEIT = "F"
}

intEnum Level {
    LOW = 1
    HIGH = 2
}

@error("client")
@httpError(404)
structure NoSuchCity {
    message: String
}

@error("server")
@retryable(throttling: true)
structure Throttled {
    message: String
}
