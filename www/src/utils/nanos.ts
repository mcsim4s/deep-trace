export function format(nanos: number): string {
    if (nanos < 1000) {
        return `${nanos}ns`;
    } else if (nanos < 1000000) {
        const micro = (nanos / 1000).toFixed(1)
        return `${micro}μs`
    } else if (nanos < 1000000000) {
        const milli = (nanos / 1000000).toFixed(1)
        return `${milli}ms`
    } else {

        const milli = (nanos / 1000000000).toFixed(1)
        return `${milli}s`
    }
}