/**
 * Copyright (c) 2017 Jakub Sapalski
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package pl.betoncraft.flier.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import pl.betoncraft.flier.api.core.Modification;
import pl.betoncraft.flier.api.core.Modifier;

/**
 * Manages modifications to the properties.
 *
 * @author Jakub Sapalski
 */
public class ModificationManager {
	
	private Set<Modification> mods = new HashSet<>();
	private Map<String, SpecializedModifier> compiled = new HashMap<>();
	
	private class SpecializedModifier {
		private String string = null;
		private Double number = null;
		private Boolean bool = null;
		private double multi = 1;
		private double bonus = 0;
	}
	
	public void clear() {
		mods.clear();
		compiled.clear();
	}
	
	public void addModification(Modification mod) {
		if (mods.add(mod)) {
			compile();
		}
	}
	
	public void removeModification(Modification mod) {
		if (mods.remove(mod)) {
			compile();
		}
	}
	
	private void compile() {
		compiled.clear();
		for (Modification mod : mods) {
			for (Modifier m : mod.getModifiers()) {
				SpecializedModifier spec = getSpec(m.getProperty());
				// parse multiplying and adding
				String value = m.getValue();
				boolean notValue = false;
				List<String> parts = value.contains(",") ?
						Arrays.asList(value.split(",")).stream().map(s -> s.trim()).collect(Collectors.toList()) :
						Arrays.asList(new String[]{value});
				for (String part : parts) {
					if (part.startsWith("*(") && part.endsWith(")")) {
						try {
							spec.multi += Double.parseDouble(part.substring(2, part.length() - 1));
							notValue = true;
						} catch (NumberFormatException e) {
							// nothing, this probably is just text
						}
					} else if (part.startsWith("+(") && part.endsWith(")")) {
						try {
							spec.bonus += Double.parseDouble(part.substring(2, part.length() - 1));
							notValue = true;
						} catch (NumberFormatException e) {
							// nothing, this probably is just text
						}
					}
				}
				// if it's not a number modifier then it's a value and needs to be saved
				if (!notValue) {
					spec.string = value;
					try {
						spec.number = Double.parseDouble(value);
					} catch (NumberFormatException e) {
						// not a number, huh
					}
					if (value.equals("true") || value.equals("false")) {
						spec.bool = Boolean.parseBoolean(value);
					}
				}
			}
		}
	}
	
	public double modifyNumber(String property, double value) {
		SpecializedModifier spec = getSpec(property);
		if (spec.number != null) {
			value = spec.number;
		}
		value *= spec.multi;
		value += spec.bonus;
		return value;
	}
	
	public String modifyString(String property, String value) {
		SpecializedModifier spec = getSpec(property);
		if (spec.string != null) {
			value = spec.string;
		}
		return value;
	}
	
	public boolean modifyBoolean(String property, boolean bool) {
		SpecializedModifier spec = getSpec(property);
		if (spec.bool != null) {
			bool = spec.bool;
		}
		return bool;
	}

	@SuppressWarnings("unchecked")
	public <T extends Enum<T>> T modifyEnum(String property, T en) {
		SpecializedModifier spec = getSpec(property);
		if (spec.string != null) {
			try {
				en = (T) Enum.valueOf(en.getClass(), spec.string);
			} catch (IllegalArgumentException e) {
				// not an enum, huh
			}
		}
		return en;
	}
	
	private SpecializedModifier getSpec(String property) {
		return compiled.computeIfAbsent(property, k -> new SpecializedModifier());
	}

}
