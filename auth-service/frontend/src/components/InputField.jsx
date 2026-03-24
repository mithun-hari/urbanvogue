function InputField({ id, label, type = 'text', value, onChange, error, placeholder, required }) {
  return (
    <div className="input-group">
      <label htmlFor={id} className="input-label">
        {label}
        {required && <span className="required-star"> *</span>}
      </label>
      <input
        id={id}
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        className={`input-field${error ? ' input-error' : ''}`}
        required={required}
        autoComplete={type === 'password' ? 'current-password' : undefined}
      />
      {error && <span className="error-msg">{error}</span>}
    </div>
  )
}

export default InputField
