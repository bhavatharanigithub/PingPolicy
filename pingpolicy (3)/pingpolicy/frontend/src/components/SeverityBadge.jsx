import React from 'react';

export default function SeverityBadge({ severity }) {
  return <span className={`badge badge-${severity}`}>{severity}</span>;
}
