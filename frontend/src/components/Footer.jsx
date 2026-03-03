import { FaGithub, FaLinkedin } from "react-icons/fa";
import { FaXTwitter } from "react-icons/fa6";
import { socialLinks, navLinks } from "../constants/data";

const socials = [
  { icon: FaGithub, href: socialLinks.github, label: "GitHub" },
  { icon: FaLinkedin, href: socialLinks.linkedin, label: "LinkedIn" },
  { icon: FaXTwitter, href: socialLinks.x, label: "X"},
];

export default function Footer() {
  return (
    <footer className="border-t border-border">
      <div className="mx-auto max-w-6xl px-6 py-12">
        <div className="flex flex-col md:flex-row items-center justify-between gap-8">
          <div>
            <a href="#home" className="text-lg font-bold text-fg">
              skyPath<span className="text-accent">.</span>
            </a>
            <p className="mt-1 text-sm text-fg-muted">
              Software Engineer at Salesforce.
            </p>
          </div>

          <nav>
            <ul className="flex flex-wrap items-center justify-center gap-6">
              {navLinks.map((link) => (
                <li key={link.name}>
                  <a
                    href={link.href}
                    className="text-sm text-fg-secondary hover:text-accent transition-colors"
                  >
                    {link.name}
                  </a>
                </li>
              ))}
            </ul>
          </nav>

          <div className="flex items-center gap-2">
            {socials.map(({ icon: Icon, href, label }) => (
              <a
                key={label}
                href={href}
                target="_blank"
                rel="noopener noreferrer"
                className="p-2.5 rounded-xl text-fg-muted hover:text-accent hover:bg-accent/5 transition-all"
                aria-label={label}
              >
                <Icon size={18} />
              </a>
            ))}
          </div>
        </div>

        <div className="mt-10 pt-6 border-t border-border flex flex-col sm:flex-row items-center justify-between gap-4">
          <p className="text-xs text-fg-muted">
            &copy; {new Date().getFullYear()} All rights reserved.
          </p>
          <p className="text-xs text-fg-muted">
            Designed & built with React + Tailwind CSS
          </p>
        </div>
      </div>
    </footer>
  );
}
