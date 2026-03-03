import { useTheme } from "./hooks/useTheme";
import Navbar from "./components/Navbar";
import Hero from "./components/Hero";
import Footer from "./components/Footer";

export default function App() {
  const { theme, toggle } = useTheme();

  return (
    <div className="min-h-screen bg-surface text-fg transition-colors duration-300">
      <Navbar theme={theme} toggleTheme={toggle} />
      <main>
        <Hero />
      </main>
      <Footer />
    </div>
  );
}
