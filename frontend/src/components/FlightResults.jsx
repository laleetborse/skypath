import { useState, useMemo } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { FiClock, FiArrowRight, FiChevronDown, FiChevronUp, FiDollarSign } from "react-icons/fi";
import { HiOutlineLocationMarker } from "react-icons/hi";

function formatDuration(minutes) {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m}m`;
  if (m === 0) return `${h}h`;
  return `${h}h ${m}m`;
}

function formatTime(dateTimeStr) {
  const d = new Date(dateTimeStr);
  return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", hour12: false });
}

function formatDate(dateTimeStr) {
  const d = new Date(dateTimeStr);
  return d.toLocaleDateString([], { month: "short", day: "numeric" });
}

function getStopsLabel(segments) {
  if (segments.length === 1) return "Direct";
  return `${segments.length - 1} stop${segments.length > 2 ? "s" : ""}`;
}

function getStopsColor(segments) {
  if (segments.length === 1) return "text-emerald";
  if (segments.length === 2) return "text-orange";
  return "text-rose";
}

function ItineraryCard({ itinerary, index }) {
  const [expanded, setExpanded] = useState(false);
  const { segments, totalDurationMinutes, totalPrice } = itinerary;
  const firstFlight = segments[0].flight;
  const lastFlight = segments[segments.length - 1].flight;

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, delay: index * 0.05 }}
      className="rounded-xl bg-surface border border-border hover:border-accent/30 hover:shadow-md transition-all"
    >
      <button
        type="button"
        onClick={() => setExpanded(!expanded)}
        className="w-full p-5 cursor-pointer"
      >
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-3">
              <div className="text-left">
                <p className="text-lg font-bold text-fg">{formatTime(firstFlight.departureTime)}</p>
                <p className="text-xs text-fg-muted">{firstFlight.origin}</p>
              </div>

              <div className="flex-1 flex flex-col items-center gap-1 px-2">
                <p className="text-xs text-fg-muted">{formatDuration(totalDurationMinutes)}</p>
                <div className="w-full flex items-center gap-1">
                  <div className="w-2 h-2 rounded-full border-2 border-accent shrink-0" />
                  <div className="flex-1 h-px bg-border relative">
                    {segments.length > 1 &&
                      segments.slice(0, -1).map((seg, i) => (
                        <div
                          key={i}
                          className="absolute top-1/2 -translate-y-1/2 w-1.5 h-1.5 rounded-full bg-fg-muted"
                          style={{ left: `${((i + 1) / segments.length) * 100}%` }}
                        />
                      ))}
                  </div>
                  <div className="w-2 h-2 rounded-full bg-accent shrink-0" />
                </div>
                <p className={`text-xs font-medium ${getStopsColor(segments)}`}>
                  {getStopsLabel(segments)}
                </p>
              </div>

              <div className="text-right">
                <p className="text-lg font-bold text-fg">{formatTime(lastFlight.arrivalTime)}</p>
                <p className="text-xs text-fg-muted">{lastFlight.destination}</p>
              </div>
            </div>

            <div className="mt-2 flex items-center gap-2 text-xs text-fg-muted">
              <span>{firstFlight.airline}</span>
              <span className="w-1 h-1 rounded-full bg-fg-muted" />
              <span>{segments.map((s) => s.flight.flightNumber).join(", ")}</span>
            </div>
          </div>

          <div className="flex items-center gap-4 sm:border-l sm:border-border sm:pl-5">
            <div className="text-right">
              <p className="text-xl font-bold text-fg">${totalPrice.toFixed(0)}</p>
              <p className="text-xs text-fg-muted">per person</p>
            </div>
            <span className="text-fg-muted">
              {expanded ? <FiChevronUp size={18} /> : <FiChevronDown size={18} />}
            </span>
          </div>
        </div>
      </button>

      <AnimatePresence>
        {expanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="overflow-hidden"
          >
            <div className="px-5 pb-5 pt-0 border-t border-border-dim">
              <div className="mt-4 space-y-4">
                {segments.map((segment, i) => (
                  <div key={i}>
                    {segment.layoverMinutes > 0 && (
                      <div className="flex items-center gap-2 mb-3 ml-5 pl-5 border-l-2 border-dashed border-fg-muted/30 py-2">
                        <FiClock size={14} className="text-fg-muted" />
                        <span className="text-xs text-fg-muted">
                          {formatDuration(segment.layoverMinutes)} layover in {segment.flight.origin}
                        </span>
                      </div>
                    )}
                    <div className="flex items-start gap-3">
                      <div className="flex flex-col items-center gap-1 mt-1">
                        <HiOutlineLocationMarker size={14} className="text-accent" />
                        <div className="w-px flex-1 bg-border min-h-8" />
                        <HiOutlineLocationMarker size={14} className="text-accent" />
                      </div>
                      <div className="flex-1 space-y-3">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-sm font-semibold text-fg">
                              {formatTime(segment.flight.departureTime)}{" "}
                              <span className="text-fg-muted font-normal">
                                {formatDate(segment.flight.departureTime)}
                              </span>
                            </p>
                            <p className="text-xs text-fg-muted">{segment.flight.origin}</p>
                          </div>
                          <div className="text-right text-xs text-fg-muted">
                            <p>{segment.flight.flightNumber}</p>
                            <p>{segment.flight.aircraft}</p>
                          </div>
                        </div>
                        <div>
                          <p className="text-sm font-semibold text-fg">
                            {formatTime(segment.flight.arrivalTime)}{" "}
                            <span className="text-fg-muted font-normal">
                              {formatDate(segment.flight.arrivalTime)}
                            </span>
                          </p>
                          <p className="text-xs text-fg-muted">{segment.flight.destination}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

const SORT_OPTIONS = [
  { key: "duration", label: "Fastest", icon: <FiClock size={14} /> },
  { key: "price", label: "Cheapest", icon: <FiDollarSign size={14} /> },
  { key: "departure", label: "Departure", icon: <FiArrowRight size={14} /> },
];

function sortResults(items, sortBy) {
  const sorted = [...items];
  switch (sortBy) {
    case "price":
      return sorted.sort((a, b) => a.totalPrice - b.totalPrice);
    case "departure":
      return sorted.sort((a, b) => {
        const aTime = new Date(a.segments[0].flight.departureTime).getTime();
        const bTime = new Date(b.segments[0].flight.departureTime).getTime();
        return aTime - bTime;
      });
    case "duration":
    default:
      return sorted.sort((a, b) => a.totalDurationMinutes - b.totalDurationMinutes);
  }
}

export default function FlightResults({ results, loading }) {
  const [sortBy, setSortBy] = useState("duration");

  const sorted = useMemo(
    () => (results ? sortResults(results, sortBy) : []),
    [results, sortBy]
  );

  if (loading) {
    return (
      <div className="mt-8 flex flex-col items-center gap-4 py-16">
        <div className="relative w-12 h-12">
          <span className="absolute inset-0 border-3 border-accent/20 rounded-full" />
          <span className="absolute inset-0 border-3 border-transparent border-t-accent rounded-full animate-spin" />
        </div>
        <p className="text-sm text-fg-muted">Finding the best routes...</p>
      </div>
    );
  }

  if (!results) return null;

  if (results.length === 0) {
    return (
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="mt-8 text-center py-16"
      >
        <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-surface-el flex items-center justify-center">
          <FiArrowRight size={24} className="text-fg-muted" />
        </div>
        <p className="text-fg-secondary font-medium">No flights found</p>
        <p className="text-sm text-fg-muted mt-1">Try different airports or dates</p>
      </motion.div>
    );
  }

  const cheapest = Math.min(...results.map((r) => r.totalPrice));
  const fastest = Math.min(...results.map((r) => r.totalDurationMinutes));

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="mt-8"
    >
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 mb-4">
        <p className="text-sm text-fg-secondary">
          <span className="font-semibold text-fg">{results.length}</span>{" "}
          {results.length === 1 ? "route" : "routes"} found
        </p>

        <div className="flex items-center gap-1 p-1 rounded-xl bg-surface-el border border-border">
          {SORT_OPTIONS.map((opt) => (
            <button
              key={opt.key}
              type="button"
              onClick={() => setSortBy(opt.key)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all cursor-pointer ${
                sortBy === opt.key
                  ? "bg-accent text-white shadow-sm"
                  : "text-fg-muted hover:text-fg"
              }`}
            >
              {opt.icon}
              <span>{opt.label}</span>
              {opt.key === "price" && sortBy !== "price" && (
                <span className="text-emerald ml-0.5">${cheapest.toFixed(0)}</span>
              )}
              {opt.key === "duration" && sortBy !== "duration" && (
                <span className="text-accent-light ml-0.5">{formatDuration(fastest)}</span>
              )}
            </button>
          ))}
        </div>
      </div>

      <div className="space-y-3">
        {sorted.map((itinerary, i) => (
          <ItineraryCard key={i} itinerary={itinerary} index={i} />
        ))}
      </div>
    </motion.div>
  );
}
