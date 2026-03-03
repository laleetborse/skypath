import { motion } from "framer-motion";
import FlightSearch from "./FlightSearch";

export default function Hero() {
  return (
    <section
      id="home"
      className="isolate relative min-h-screen flex flex-col overflow-hidden pt-24 pb-10"
    >
      <div className="absolute inset-0 -z-10 pointer-events-none overflow-hidden">
        <div
          className="absolute -top-1/4 -right-1/6 w-[60%] h-[70%] opacity-30 dark:opacity-15"
          style={{
            background: "conic-gradient(from 160deg at 50% 50%, #7c3aed, #f200b9, #ff3681, transparent)",
            filter: "blur(90px)",
            borderRadius: "40% 60% 55% 45% / 55% 40% 60% 45%",
          }}
        />
        <div
          className="absolute top-1/4 -right-1/12 w-[45%] h-[50%] opacity-25 dark:opacity-12"
          style={{
            background: "conic-gradient(from 220deg at 40% 60%, #ff3681, #ff8456, #ffc349, transparent)",
            filter: "blur(80px)",
            borderRadius: "55% 45% 40% 60% / 45% 55% 45% 55%",
          }}
        />
        <div
          className="absolute -bottom-1/6 right-1/6 w-[40%] h-[45%] opacity-20 dark:opacity-10"
          style={{
            background: "conic-gradient(from 300deg at 60% 40%, #ffc349, #f9f871, #ff8456, transparent)",
            filter: "blur(70px)",
            borderRadius: "45% 55% 60% 40% / 50% 45% 55% 50%",
          }}
        />
      </div>

      <div className="mx-auto max-w-5xl w-full px-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="text-center mb-10"
        >
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold tracking-tight text-fg">
            Find your perfect
            <span className="text-accent"> flight</span>
          </h1>
          <p className="mt-4 text-lg text-fg-secondary max-w-2xl mx-auto">
            Compare routes and prices across hundreds of flights.
            Discover the fastest and cheapest way to your destination.
          </p>
        </motion.div>

        <FlightSearch />
      </div>
    </section>
  );
}
