import type { ReactNode, SVGProps } from "react";

type IconButtonProps = {
  label: string;
  disabled?: boolean;
  className?: string;
  onClick: () => void;
  children: ReactNode;
};

export function IconButton(props: IconButtonProps) {
  return (
    <button
      type="button"
      className={`icon-button ${props.className ?? ""}`.trim()}
      aria-label={props.label}
      title={props.label}
      disabled={props.disabled}
      onClick={props.onClick}
    >
      {props.children}
    </button>
  );
}

type GlyphProps = {
  className?: string;
};

type SvgProps = SVGProps<SVGSVGElement> & GlyphProps;

const GREEN = "#0F6E56";
const RED = "#8B2E2E";

function FlatSvg(props: SvgProps) {
  const { className, children, ...rest } = props;
  return (
    <svg
      className={`icon-glyph ${className ?? ""}`.trim()}
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
      focusable="false"
      {...rest}
    >
      {children}
    </svg>
  );
}

/** Stroke inherits the button border color via CSS `color`. */
export function IconAdd(props: GlyphProps = {}) {
  return (
    <FlatSvg className={props.className}>
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="2" />
      <path d="M12 7v10M7 12h10" stroke="currentColor" strokeWidth="2.25" strokeLinecap="round" />
    </FlatSvg>
  );
}

export function IconEdit(props: GlyphProps = {}) {
  return (
    <FlatSvg className={props.className}>
      <rect x="4" y="3.5" width="12" height="15" rx="2" stroke="currentColor" strokeWidth="2" />
      <path
        d="M13.8 15.8 19.2 6.2l-2.1-1.2-5.4 9.6.3 2.2 1.8-1z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinejoin="round"
      />
    </FlatSvg>
  );
}

export function IconRemove(props: GlyphProps = {}) {
  return (
    <FlatSvg className={props.className}>
      <path d="M8 7h8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <path
        d="M9 7V6a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v1"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <path
        d="M7.5 7h9l-.7 11.2A2 2 0 0 1 13.8 20h-3.6a2 2 0 0 1-2-1.8L7.5 7Z"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinejoin="round"
      />
      <path
        d="M10 10.5v6M14 10.5v6"
        stroke="currentColor"
        strokeWidth="1.75"
        strokeLinecap="round"
      />
    </FlatSvg>
  );
}

export function IconSave(props: GlyphProps = {}) {
  return (
    <FlatSvg className={props.className}>
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="2" />
      <path
        d="M7.5 12.2 10.6 15.4 16.5 8.8"
        stroke="currentColor"
        strokeWidth="2.25"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </FlatSvg>
  );
}

export function IconClose(props: GlyphProps = {}) {
  return (
    <FlatSvg className={props.className}>
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="2" />
      <path d="M8 8l8 8M16 8l-8 8" stroke="currentColor" strokeWidth="2.25" strokeLinecap="round" />
    </FlatSvg>
  );
}

export function IconLogout(props: GlyphProps = {}) {
  return (
    <FlatSvg className={props.className}>
      <path
        d="M10 5H7a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h3"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <path
        d="M12 12h8M16.5 8.5 20 12l-3.5 3.5"
        stroke="currentColor"
        strokeWidth="2.25"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </FlatSvg>
  );
}

export function IconChevronLeft(props: GlyphProps = {}) {
  return (
    <FlatSvg className={props.className}>
      <path
        d="M14.5 6 9 12l5.5 6"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </FlatSvg>
  );
}

export function IconChevronRight(props: GlyphProps = {}) {
  return (
    <FlatSvg className={props.className}>
      <path
        d="M9.5 6 15 12l-5.5 6"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </FlatSvg>
  );
}

export function IconAlertError(props: GlyphProps = {}) {
  return (
    <FlatSvg className={props.className}>
      <path d="M12 3.8 21 19.5H3L12 3.8Z" stroke={RED} strokeWidth="2" strokeLinejoin="round" />
      <path d="M12 9v5" stroke={RED} strokeWidth="2.25" strokeLinecap="round" />
      <circle cx="12" cy="16.5" r="1.15" fill={RED} />
    </FlatSvg>
  );
}

export function IconAuthLogin(props: GlyphProps = {}) {
  return (
    <FlatSvg className={props.className}>
      <rect x="9" y="4" width="11" height="16" rx="2.5" stroke="currentColor" strokeWidth="2" />
      <path
        d="M3.5 12H12M8.5 8.5 12 12l-3.5 3.5"
        stroke="currentColor"
        strokeWidth="2.25"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </FlatSvg>
  );
}

export function IconAuthSignup(props: GlyphProps = {}) {
  return (
    <FlatSvg className={props.className}>
      <circle cx="10" cy="8" r="3.2" stroke="currentColor" strokeWidth="2" />
      <path
        d="M4.2 18.5c.6-3.2 2.8-4.8 5.8-4.8s5.2 1.6 5.8 4.8"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
      <circle cx="17.5" cy="15.5" r="3.4" stroke="currentColor" strokeWidth="2" />
      <path
        d="M17.5 13.8v3.4M15.8 15.5h3.4"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
      />
    </FlatSvg>
  );
}

export function BrandMark(props: GlyphProps = {}) {
  return (
    <FlatSvg className={`brand-mark ${props.className ?? ""}`.trim()} viewBox="0 0 24 24">
      <path
        d="M12 2.8 19.2 6.2v5.4c0 4.6-3 8.4-7.2 9.6-4.2-1.2-7.2-5-7.2-9.6V6.2L12 2.8Z"
        stroke={GREEN}
        strokeWidth="2"
        strokeLinejoin="round"
      />
      <circle cx="12" cy="12" r="4.2" stroke={GREEN} strokeWidth="2" fill="none" />
      <rect
        x="10.2"
        y="11.2"
        width="3.6"
        height="3.2"
        rx="0.6"
        stroke={GREEN}
        strokeWidth="1.4"
        fill="none"
      />
      <path
        d="M11 11.2v-1.2a1 1 0 0 1 2 0v1.2"
        stroke={GREEN}
        strokeWidth="1.4"
        strokeLinecap="round"
      />
    </FlatSvg>
  );
}

/** @deprecated Use named icons from IconSet */
export const IconPlus = IconAdd;
export const IconPencil = IconEdit;
export const IconTrash = IconRemove;
export const IconCheck = IconSave;
export const IconX = IconClose;
