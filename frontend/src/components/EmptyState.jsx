import Icon from './Icon.jsx';

export default function EmptyState({ title, children }) {
  return <section className="empty-state"><span className="empty-icon"><Icon name="bag" /></span><h2>{title}</h2>{children}</section>;
}
