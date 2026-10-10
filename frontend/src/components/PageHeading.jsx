export default function PageHeading({ title, description, eyebrow, children }) {
  return <div className="page-heading">
    <div>{eyebrow && <p className="eyebrow">{eyebrow}</p>}<h1>{title}</h1>{description && <p>{description}</p>}</div>
    {children && <div className="actions">{children}</div>}
  </div>;
}
