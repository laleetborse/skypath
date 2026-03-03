export default function Hero() {
  return (
    <section
      id="home"
      className="isolate relative min-h-screen flex flex-col justify-between overflow-hidden pt-20 pb-10"
    >
      {/* Gradient background – right side */}
      <div className="absolute inset-0 -z-10 pointer-events-none overflow-hidden">
        {/* Top-right blob */}
        <div
          className="absolute -top-1/4 -right-1/6 w-[60%] h-[70%] opacity-30 dark:opacity-15"
          style={{
            background: "conic-gradient(from 160deg at 50% 50%, #7c3aed, #f200b9, #ff3681, transparent)",
            filter: "blur(90px)",
            borderRadius: "40% 60% 55% 45% / 55% 40% 60% 45%",
          }}
        />
        {/* Mid-right blob */}
        <div
          className="absolute top-1/4 -right-1/12 w-[45%] h-[50%] opacity-25 dark:opacity-12"
          style={{
            background: "conic-gradient(from 220deg at 40% 60%, #ff3681, #ff8456, #ffc349, transparent)",
            filter: "blur(80px)",
            borderRadius: "55% 45% 40% 60% / 45% 55% 45% 55%",
          }}
        />
        {/* Bottom-right glow */}
        <div
          className="absolute -bottom-1/6 right-1/6 w-[40%] h-[45%] opacity-20 dark:opacity-10"
          style={{
            background: "conic-gradient(from 300deg at 60% 40%, #ffc349, #f9f871, #ff8456, transparent)",
            filter: "blur(70px)",
            borderRadius: "45% 55% 60% 40% / 50% 45% 55% 50%",
          }}
        />
      </div>
    </section>
  );
}
