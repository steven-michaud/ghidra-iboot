package iboot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ghidra.app.util.Option;
import ghidra.app.util.bin.ByteProvider;
import ghidra.app.util.importer.MessageLog;
import ghidra.app.util.opinion.AbstractLibrarySupportLoader;
import ghidra.app.util.opinion.LoadSpec;
import ghidra.framework.model.DomainObject;
import ghidra.program.flatapi.FlatProgramAPI;
import ghidra.program.model.address.Address;
import ghidra.program.model.lang.LanguageCompilerSpecPair;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.Memory;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.util.exception.CancelledException;
import ghidra.util.exception.InvalidInputException;
import ghidra.util.task.TaskMonitor;

//import ghidra.framework.Application;

//class Config {
//	//public static final boolean NEWAPI =
//	//	(Application.getApplicationVersion().compareTo("12.0.0") >= 0);
//	static final boolean NEWAPI = true;
//}

// We don't need to override any methods where we won't be making changes --
// where we'd just be calling super(). Since we have two, version-specific
// load() methods, we can't use '@Override' for either of them. This API
// changed as of Ghidra version 12.0.0.
public class iBootLoader extends AbstractLibrarySupportLoader {
	@Override
	public String getName() {
		return "iBoot Image";
	}

	@Override
	public Collection<LoadSpec> findSupportedLoadSpecs(ByteProvider provider) throws IOException {
		var result = new ArrayList<LoadSpec>();
		try {
			iBootInfo info = new iBootInfo(provider);
			if (info.is64Bit()) {
				result.add(new LoadSpec(this, info.getBaseAddress(),
						new LanguageCompilerSpecPair("AARCH64:LE:64:v8A", "default"),
						true));
			}
		} catch (InvalidInputException exception) {
			// The binary is not an iBoot image, and thus can't be loaded by this loader.
		}
		return result;
	}

	// Prior to Ghidra 12.0.0.
	protected void load(ByteProvider provider, LoadSpec loadSpec, List<Option> options,
						Program program, TaskMonitor monitor, MessageLog log) {
		FlatProgramAPI flatProgramAPI = new FlatProgramAPI(program, monitor);
		Memory memory = program.getMemory();
		monitor.setMessage("Loading iBoot stage...");
		try {
			Address imageBase = program.getAddressFactory().getDefaultAddressSpace().getAddress(loadSpec.getDesiredImageBase());
			MemoryBlock imageBlock = memory.createInitializedBlock("iBoot", imageBase, provider.length(), (byte) 0, monitor, false);
			imageBlock.setRead(true);
			imageBlock.setExecute(true);
			memory.setBytes(imageBase, provider.readBytes(0, provider.length()));
			flatProgramAPI.addEntryPoint(imageBase);
			flatProgramAPI.disassemble(imageBase);
		} catch (Exception exception) {
			log.appendException(exception);
		}
	}

	// As of Ghidra 12.0.0.
	protected void load(Program program, ImporterSettings settings)
			throws IOException, CancelledException {
		MessageLog log = settings.log();
		ByteProvider provider = settings.provider();
		TaskMonitor monitor = settings.monitor();
		LoadSpec loadSpec = settings.loadSpec();
		FlatProgramAPI flatProgramAPI = new FlatProgramAPI(program, monitor);
		Memory memory = program.getMemory();
		monitor.setMessage("Loading iBoot stage...");
		try {
			Address imageBase = program.getAddressFactory().getDefaultAddressSpace().getAddress(loadSpec.getDesiredImageBase());
			MemoryBlock imageBlock = memory.createInitializedBlock("iBoot", imageBase, provider.length(), (byte) 0, monitor, false);
			imageBlock.setRead(true);
			imageBlock.setExecute(true);
			memory.setBytes(imageBase, provider.readBytes(0, provider.length()));
			flatProgramAPI.addEntryPoint(imageBase);
			flatProgramAPI.disassemble(imageBase);
		} catch (Exception exception) {
			log.appendException(exception);
		}
	}
}
