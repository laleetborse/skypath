import { useState } from "react";
import { motion } from "framer-motion";
import { HiOutlineSwitchHorizontal } from "react-icons/hi";
import { FiMapPin, FiCalendar, FiSearch } from "react-icons/fi";
import AirportInput from "./AirportInput";
import FlightResults from "./FlightResults";
import { searchFlights } from "../services/api";

export default function FlightSearch() {
  const [origin, setOrigin] = useState(null);
  const [destination, setDestination] = useState(null);
  const [date, setDate] = useState("");
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  function handleSwap() {
    setOrigin(destination);
    setDestination(origin);
  }

  async function handleSearch(e) {
    e.preventDefault();
    setError("");

    if (!origin || !destination) {
      setError("Please select both origin and destination airports.");
      return;
    }
    if (!date) {
      setError("Please select a departure date.");
      return;
    }

    setLoading(true);
    setResults(null);
    try {
      const data = await searchFlights(origin.code, destination.code, date);
      setResults(data);
    } catch {
      setError("Something went wrong. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="w-full">
      <motion.form
        onSubmit={handleSearch}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.2 }}
        className="relative rounded-2xl bg-surface/80 backdrop-blur-xl border border-border p-6 shadow-lg"
      >
        <div className="flex flex-col lg:flex-row items-end gap-3">
          <AirportInput
            label="From"
            icon={<FiMapPin />}
            value={origin}
            onChange={setOrigin}
            placeholder="City or airport"
          />

          <button
            type="button"
            onClick={handleSwap}
            className="shrink-0 mb-0.5 p-2.5 rounded-full border border-border hover:border-accent hover:bg-accent/5 text-fg-muted hover:text-accent transition-all cursor-pointer self-center lg:self-end lg:mb-0"
            aria-label="Swap origin and destination"
          >
            <HiOutlineSwitchHorizontal size={18} />
          </button>

          <AirportInput
            label="To"
            icon={<FiMapPin />}
            value={destination}
            onChange={setDestination}
            placeholder="City or airport"
          />

          <div className="flex-1 min-w-0 lg:max-w-[180px]">
            <label className="block text-xs font-semibold text-fg-muted mb-1.5 uppercase tracking-wider">
              Date
            </label>
            <div className="relative">
              <span className="absolute left-4 top-1/2 -translate-y-1/2 text-fg-muted text-lg">
                <FiCalendar />
              </span>
              <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="w-full pl-11 pr-4 py-3 rounded-xl bg-surface-el border border-border text-sm text-fg focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent/30 transition-all appearance-none"
              />
            </div>
          </div>

          <motion.button
            type="submit"
            disabled={loading}
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            className="shrink-0 px-8 py-3 rounded-xl bg-accent hover:bg-accent-dark text-white text-sm font-semibold shadow-lg shadow-accent/25 hover:shadow-accent/40 transition-all disabled:opacity-60 disabled:cursor-not-allowed flex items-center gap-2 cursor-pointer"
          >
            {loading ? (
              <span className="block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              <FiSearch size={16} />
            )}
            {loading ? "Searching..." : "Search"}
          </motion.button>
        </div>

        {error && (
          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="mt-3 text-sm text-rose"
          >
            {error}
          </motion.p>
        )}
      </motion.form>

      <FlightResults results={results} loading={loading} />
    </div>
  );
}
