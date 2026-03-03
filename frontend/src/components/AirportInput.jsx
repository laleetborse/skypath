import { useState, useEffect, useRef } from "react";
import { searchAirports } from "../services/api";
import { motion, AnimatePresence } from "framer-motion";

export default function AirportInput({ label, icon, value, onChange, placeholder }) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const wrapperRef = useRef(null);
  const debounceRef = useRef(null);

  useEffect(() => {
    if (!query || query.length < 1) {
      setResults([]);
      return;
    }

    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      setLoading(true);
      try {
        const data = await searchAirports(query);
        setResults(data);
        setOpen(true);
      } catch {
        setResults([]);
      } finally {
        setLoading(false);
      }
    }, 200);

    return () => clearTimeout(debounceRef.current);
  }, [query]);

  useEffect(() => {
    function handleClickOutside(e) {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  function handleSelect(airport) {
    onChange(airport);
    setQuery("");
    setOpen(false);
  }

  function handleClear() {
    onChange(null);
    setQuery("");
    setResults([]);
  }

  return (
    <div ref={wrapperRef} className="relative flex-1 min-w-0">
      <label className="block text-xs font-semibold text-fg-muted mb-1.5 uppercase tracking-wider">
        {label}
      </label>
      {value ? (
        <button
          type="button"
          onClick={handleClear}
          className="w-full flex items-center gap-3 px-4 py-3 rounded-xl bg-surface-el border border-border text-left hover:border-accent/50 transition-colors cursor-pointer"
        >
          <span className="text-accent text-lg shrink-0">{icon}</span>
          <div className="min-w-0">
            <p className="text-sm font-semibold text-fg truncate">
              {value.city} ({value.code})
            </p>
            <p className="text-xs text-fg-muted truncate">{value.name}</p>
          </div>
        </button>
      ) : (
        <div className="relative">
          <span className="absolute left-4 top-1/2 -translate-y-1/2 text-fg-muted text-lg">
            {icon}
          </span>
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onFocus={() => query.length >= 1 && results.length > 0 && setOpen(true)}
            placeholder={placeholder}
            className="w-full pl-11 pr-4 py-3 rounded-xl bg-surface-el border border-border text-sm text-fg placeholder:text-fg-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent/30 transition-all"
          />
          {loading && (
            <span className="absolute right-3 top-1/2 -translate-y-1/2">
              <span className="block w-4 h-4 border-2 border-accent/30 border-t-accent rounded-full animate-spin" />
            </span>
          )}
        </div>
      )}

      <AnimatePresence>
        {open && results.length > 0 && (
          <motion.ul
            initial={{ opacity: 0, y: -4 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -4 }}
            transition={{ duration: 0.15 }}
            className="absolute z-50 top-full mt-1 w-full max-h-60 overflow-y-auto rounded-xl bg-surface border border-border shadow-xl"
          >
            {results.map((airport) => (
              <li key={airport.code}>
                <button
                  type="button"
                  onClick={() => handleSelect(airport)}
                  className="w-full flex items-center gap-3 px-4 py-3 text-left hover:bg-accent/5 transition-colors cursor-pointer"
                >
                  <span className="w-10 h-10 rounded-lg bg-accent/10 text-accent flex items-center justify-center text-xs font-bold shrink-0">
                    {airport.code}
                  </span>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-fg truncate">{airport.city}, {airport.country}</p>
                    <p className="text-xs text-fg-muted truncate">{airport.name}</p>
                  </div>
                </button>
              </li>
            ))}
          </motion.ul>
        )}
      </AnimatePresence>
    </div>
  );
}
