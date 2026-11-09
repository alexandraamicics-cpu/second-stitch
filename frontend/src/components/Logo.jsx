// The Second Stitch mark: a black circle sticker with a stitched ring and "SS", plus the name.
export default function Logo() {
  return (
    <div className="logo">
      <svg className="logo-mark" viewBox="0 0 100 100" aria-hidden="true">
        <circle cx="50" cy="50" r="47" fill="#1f1d24" />
        <circle cx="50" cy="50" r="38" fill="none" stroke="#ffffff" strokeWidth="2.5" strokeDasharray="6 5" />
        <text
          x="50"
          y="62"
          textAnchor="middle"
          fontFamily="Bricolage Grotesque, Segoe UI, sans-serif"
          fontWeight="800"
          fontSize="34"
          fill="#ffffff"
          letterSpacing="-2"
        >
          SS
        </text>
      </svg>
      <span className="logo-name">Second Stitch</span>
    </div>
  )
}
