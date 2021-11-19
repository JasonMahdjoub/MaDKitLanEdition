/*
 * MadKitLanEdition (created by Jason MAHDJOUB (jason.mahdjoub@distri-mind.fr)) Copyright (c)
 * 2015 is a fork of MadKit and MadKitGroupExtension. 
 * 
 * Copyright or © or Copr. Jason Mahdjoub, Fabien Michel, Olivier Gutknecht, Jacques Ferber (1997)
 * 
 * jason.mahdjoub@distri-mind.fr
 * fmichel@lirmm.fr
 * olg@no-distance.net
 * ferber@lirmm.fr
 * 
 * This software is a computer program whose purpose is to
 * provide a lightweight Java library for designing and simulating Multi-Agent Systems (MAS).
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package com.distrimind.madkit.api.abstractAgent;

import com.distrimind.madkit.JUnitFunctions;
import com.distrimind.madkit.kernel.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.AGENT_CRASH;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.ALREADY_KILLED;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.ALREADY_LAUNCHED;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.NOT_YET_LAUNCHED;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.SUCCESS;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.TIMEOUT;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.distrimind.madkit.kernel.TestNGMadkit;
import com.distrimind.madkit.testing.util.agent.DoItDuringLifeCycleAgent;
import com.distrimind.madkit.testing.util.agent.FaultyAgent;
import com.distrimind.madkit.testing.util.agent.KillTargetAgent;
import com.distrimind.madkit.testing.util.agent.NormalLife;
import com.distrimind.madkit.testing.util.agent.RandomT;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 5.0.0.7
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */

public class KillAgentTest extends TestNGMadkit {

	final Agent target = new Agent() {
		@Override
		protected void activate() {
			AssertJUnit.assertEquals(SUCCESS, createGroup(GROUP));
			AssertJUnit.assertEquals(SUCCESS, requestRole(GROUP, ROLE));
		}

		@Override
		protected void liveCycle() throws InterruptedException {
			pause(10000);
			if (logger != null)
				logger.info("finishing live");
			this.killAgent(this);
		}
	};

	final AtomicInteger numberOfReadMessages = new AtomicInteger(0);

	final Agent target2 = new Agent() {
		@Override
		protected void activate() throws InterruptedException {
			super.activate();
			AssertJUnit.assertEquals(SUCCESS, requestRole(GROUP, ROLE));
		}

		@Override
		protected void liveCycle() throws InterruptedException {
			// super.liveCycle();
			try {
				waitNextMessage(2000);
			} catch (InterruptedException e) {
				waitNextMessage(2000);
			}
			numberOfReadMessages.incrementAndGet();
			try {
				sleep(3000);
			} catch (InterruptedException e) {
				sleep(3000);
			}
			waitNextMessage(2000);
			numberOfReadMessages.incrementAndGet();
			this.killAgent(this);
		}
	};

	final Agent target3 = new Agent() {
		@Override
		protected void activate() throws InterruptedException {
			super.activate();
			AssertJUnit.assertEquals(SUCCESS, requestRole(GROUP, ROLE));
		}

		@Override
		protected void liveCycle() throws InterruptedException {
			waitNextMessage(2000);
			if (numberOfReadMessages.incrementAndGet() == 1) {
				sleep(2000);
			}
		}
	};

	AbstractAgent timeOutAgent;

	@BeforeMethod
	public void setTimeOutAgent()
	{
		timeOutAgent = new AbstractAgent() {
			@Override
			protected void activate() throws InterruptedException {

				Thread.sleep(6000);
				/*
				 * try { sleep(4000); } catch(InterruptedException e) { sleep(3000); }
				 */
			}
		};
	}

	@Test
	public void returnSuccess() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				AssertJUnit.assertEquals(SUCCESS, launchAgent(target));
				AssertJUnit.assertEquals(SUCCESS, killAgent(target));
			}
		});
	}

	@Test
	public void waitPurgeMessageBoxAndReturnSuccess() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() throws InterruptedException {
				requestRole(GROUP, ROLE);
				test(target2);
				test(target3);
			}

			@Test
			void test(Agent agentToTest) throws InterruptedException {
				numberOfReadMessages.set(0);
				AssertJUnit.assertEquals(SUCCESS, launchAgent(agentToTest));
				AgentAddress aa = getAgentWithRole(GROUP, ROLE);
				AssertJUnit.assertEquals(ReturnCode.SUCCESS, sendMessage(aa, new Message()));
				AssertJUnit.assertEquals(ReturnCode.SUCCESS, sendMessage(aa, new Message()));
				AssertJUnit.assertEquals(ReturnCode.TIMEOUT,
						killAgent(agentToTest, 0, KillingType.WAIT_AGENT_PURGE_ITS_MESSAGES_BOX_BEFORE_KILLING_IT));
				pause(500);
				AssertJUnit.assertEquals(State.ZOMBIE, agentToTest.getState());
				JUnitFunctions.assertNotEquals(ReturnCode.AGENT_CRASH, sendMessage(aa, new Message()));
				AssertJUnit.assertEquals(1, numberOfReadMessages.get());
				pause(3500);
				AssertJUnit.assertEquals(2, numberOfReadMessages.get());

			}

		});
	}

	@Test
	public void returnSuccessAfterLaunchTimeOut() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				AssertJUnit.assertEquals(TIMEOUT, launchAgent(timeOutAgent, 1));
				AssertJUnit.assertEquals(SUCCESS, killAgent(timeOutAgent));
			}
		});
	}

	@Test
	public void selfKillInActivate() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				SelfKillAgent a = new SelfKillAgent(true);
				AssertJUnit.assertEquals(SUCCESS, launchAgent(a));
				TestNGMadkit.pause(this, 200);
				AssertJUnit.assertEquals(ALREADY_KILLED, killAgent(a));
				TestNGMadkit.pause(this, 200);
				assertAgentIsTerminated(a);
			}
		}, true);
	}

	@Test
	public void selfKillInActivateAndEnd() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() throws InterruptedException {
				SelfKillAgent a = new SelfKillAgent(true, false, true);
				AssertJUnit.assertEquals(SUCCESS, launchAgent(a));

				this.sleep(500);

				assertAgentIsTerminated(a);
			}
		}, true);
	}

	@Test
	public void selfKillInEnd() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				SelfKillAgent a = new SelfKillAgent(false, false, true);
				AssertJUnit.assertEquals(SUCCESS, launchAgent(a));
				TestNGMadkit.pause(this, 100);
				AssertJUnit.assertEquals(ALREADY_KILLED, killAgent(a));
				assertAgentIsTerminated(a);
			}
		}, true);
	}

	@Test
	public void selfKill() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				AssertJUnit.assertEquals(SUCCESS, launchAgent(new SelfKillAgent(true), 1));
				AssertJUnit.assertEquals(SUCCESS, launchAgent(new SelfKillAgent(false, true), 1));
				AssertJUnit.assertEquals(SUCCESS, launchAgent(new SelfKillAgent(false, false, true), 1));
				AssertJUnit.assertEquals(SUCCESS, launchAgent(new SelfKillAgent(true, false, true), 1));
			}
		});
	}

	@Test
	public void returnNOT_YET_LAUNCHEDAfterImmediateLaunch() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				AssertJUnit.assertEquals(TIMEOUT, launchAgent(timeOutAgent, 0));
				ReturnCode r = killAgent(timeOutAgent);
				AssertJUnit.assertTrue(NOT_YET_LAUNCHED == r || SUCCESS == r);
				TestNGMadkit.pause(this, 2000);
				if (r == NOT_YET_LAUNCHED) {
					AssertJUnit.assertEquals(SUCCESS, killAgent(timeOutAgent));
				}
			}
		});
	}

	@Test
	public void returnAlreadyKilled() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				AbstractAgent a = new FaultyAgent(true);
				AssertJUnit.assertEquals(AGENT_CRASH, launchAgent(a));
				TestNGMadkit.pause(this, 100);
				AssertJUnit.assertEquals(ALREADY_KILLED, killAgent(a));
				a = new FaultyAgent(false, true);
				AssertJUnit.assertEquals(SUCCESS, launchAgent(a));
				TestNGMadkit.pause(this, 100);
				AssertJUnit.assertEquals(ALREADY_KILLED, killAgent(a));
			}
		});
	}

	@Test
	public void agentCrash() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				AbstractAgent a = new FaultyAgent(true);
				AssertJUnit.assertEquals(AGENT_CRASH, launchAgent(a));
				TestNGMadkit.pause(this, 100);
			}
		});
	}

	@Test
	public void massKill() {
		addMadkitArgs("--agentLogLevel", "OFF");
		addMadkitArgs("--kernelLogLevel", "OFF");
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				setLogLevel(Level.INFO);
				int number = 600;
				ArrayList<AbstractAgent> list = new ArrayList<>(number);
				for (int i = 0; i < number; i++) {
					if (i % 100 == 0 && logger != null)
						logger.info(i + " agents launched");
					TimeOutAgent t = new TimeOutAgent();
					list.add(t);
					AssertJUnit.assertEquals(SUCCESS, launchAgent(t));
				}
				for (AbstractAgent a : list) {
					ReturnCode r = killAgent(a);
					AssertJUnit.assertTrue(ALREADY_KILLED == r || SUCCESS == r);
				}
			}
		});
	}

	@Test
	public void returnTimeOut() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				AssertJUnit.assertEquals(TIMEOUT, launchAgent(timeOutAgent, 1));
				AssertJUnit.assertEquals(ALREADY_LAUNCHED, launchAgent(timeOutAgent));
				AssertJUnit.assertEquals(SUCCESS, killAgent(timeOutAgent));
				assertAgentIsTerminated(timeOutAgent);
			}
		});
	}

	@Test
	public void returnAleradyLaunch() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				AssertJUnit.assertEquals(TIMEOUT, launchAgent(timeOutAgent, 1));
				AssertJUnit.assertEquals(ALREADY_LAUNCHED, launchAgent(timeOutAgent));
			}
		});
	}

	@Test
	public void randomLaunchAndKill() {
		addMadkitArgs("--agentLogLevel", "ALL", "--kernelLogLevel", "FINEST", "--guiLogLevel", "ALL");
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() throws InterruptedException {
				if (logger != null) {
					logger.info("******************* STARTING RANDOM LAUNCH & AGENT_KILL *******************\n");
				}
				Agent a = (Agent) launchAgent(NormalLife.class.getName(), Math.random() < .5);
				AssertJUnit.assertNotNull(a);
				ReturnCode r = killAgent(a, (int) (Math.random() * 2));
				AssertJUnit.assertTrue(SUCCESS == r || TIMEOUT == r);
				final AbstractAgent This = this;
				Runnable job = () -> {
					for (int i = 0; i < 20; i++) {
						Agent agt = (Agent) launchAgent(NormalLife.class.getName(),
Math.random() < .5);
						AssertJUnit.assertNotNull(agt);
						TestNGMadkit.pause(This, (int) (Math.random() * 1000));
						ReturnCode r2 = killAgent(agt, (int) (Math.random() * 2));
						AssertJUnit.assertTrue(SUCCESS == r2 || TIMEOUT == r2);
					}
				};
				Thread t = new Thread(job);
				t.start();
				TestNGMadkit.pause(this, 1000);
				t = new Thread(job);
				t.start();
				t.join();
			}
		});
	}

	@Test
	public void cascadeKills() {// TODO more cases
		addMadkitArgs("--agentLogLevel", "ALL");
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				Agent a = new NormalLife(false, true);
				AssertJUnit.assertEquals(SUCCESS, launchAgent(a, 1));
				AssertJUnit.assertNotNull(a);
				KillTargetAgent ka = new KillTargetAgent(a);
				launchAgent(ka);
				killAgent(ka);
				TestNGMadkit.pause(this, 500);
				assertAgentIsTerminated(ka);
				assertAgentIsTerminated(a);
			}
		});
	}

	@Test
	public void immediateKillWithTimeOut() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				Agent a = new NormalLife(false, true);
				AssertJUnit.assertEquals(SUCCESS, launchAgent(a));
				AssertJUnit.assertNotNull(a);
				AssertJUnit.assertEquals(SUCCESS, killAgent(a, 2));
				ReturnCode res = killAgent(a, 2);
				AssertJUnit.assertSame(ALREADY_KILLED, res);
				TestNGMadkit.pause(this, 1500);
				assertAgentIsTerminated(a);
			}
		});
	}

	@Test
	public void immediateKill() {
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				Agent a = new NormalLife(false, true);
				AssertJUnit.assertEquals(SUCCESS, launchAgent(a));
				TestNGMadkit.pause(this, 1000);
				AssertJUnit.assertEquals(SUCCESS, killAgent(a));
				TestNGMadkit.pause(this, 100);
				assertAgentIsTerminated(a);
				Agent b = (Agent) launchAgent(Agent.class.getName(), 10);
				killAgent(b, 0);
				TestNGMadkit.pause(this, 100);
				assertAgentIsTerminated(b);
			}
		});
	}

	@Test
	public void randomTesting() {
		RandomT.killingOn = false;
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				setLogLevel(Level.OFF);
				ArrayList<AbstractAgent> agents = new ArrayList<>();
				for (int i = 0; i < 50; i++) {
					agents.add(new RandomT());
				}
				RandomT.agents = agents;
				AssertJUnit.assertEquals(SUCCESS, launchAgent(agents.get(0), 1));
				boolean notFinished = true;
				while (notFinished) {
					if (logger != null) {
						logger.info("waiting for the end of the test");
					}
					TestNGMadkit.pause(this, 1000);
					notFinished = false;
					for (AbstractAgent randomTest : agents) {
						try {
							if (randomTest.getState() != State.TERMINATED
									&& randomTest.getState() != State.NOT_LAUNCHED) {
								notFinished = true;
								if (logger != null) {
									logger.info("Waiting termination of " + randomTest.getName() + " state is "
											+ randomTest.getState());
								}
							}
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}, false);
		RandomT.killingOn = true;
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() {
				setLogLevel(Level.OFF);
				ArrayList<AbstractAgent> agents = new ArrayList<>();
				for (int i = 0; i < 50; i++) {
					agents.add(new RandomT());
				}
				RandomT.agents = agents;
				AssertJUnit.assertEquals(SUCCESS, launchAgent(agents.get(0), 1));
				boolean notFinished = true;
				while (notFinished) {
					if (logger != null) {
						logger.info("waiting for the end of the test");
					}
					TestNGMadkit.pause(this, 1000);
					notFinished = false;
					for (AbstractAgent randomTest : agents) {
						try {
							if (randomTest.getState() != State.TERMINATED
									&& randomTest.getState() != State.NOT_LAUNCHED) {
								notFinished = true;
								if (logger != null) {
									logger.info("Waiting termination of " + randomTest.getName() + " state is "
											+ randomTest.getState());
								}
							}
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}, false);
	}

}

class TimeOutAgent extends Agent {
	@Override
	protected void liveCycle() throws InterruptedException {
		pause(1000);
	}

	@Override
	protected void end() {
	}
}

class SelfKillAgent extends DoItDuringLifeCycleAgent {

	@SuppressWarnings("unused")
	public SelfKillAgent() {
		super();
	}

	public SelfKillAgent(boolean inActivate, boolean inLive, boolean inEnd) {
		super(inActivate, inLive, inEnd);
	}

	public SelfKillAgent(boolean inActivate, boolean inLive) {
		super(inActivate, inLive);
	}

	public SelfKillAgent(boolean inActivate) {
		super(inActivate);
	}

	@Override
	public void doIt() throws InterruptedException {
		super.doIt();
		killAgent(this);
	}

}